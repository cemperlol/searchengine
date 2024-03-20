package searchengine.services.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.LastSearch;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchServiceResult;
import searchengine.model.*;
import searchengine.utils.lemmas.Lemmatizator;
import searchengine.utils.responsegenerators.SearchResponseGenerator;
import searchengine.utils.workers.HtmlWorker;
import searchengine.utils.workers.RelevanceWorker;
import searchengine.utils.workers.SnippetWorker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Lock lock = new ReentrantLock();

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    private final AtomicReference<Float> absRelevance = new AtomicReference<>(-1.0f);

    @Autowired
    public SearchServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                             LemmaRepository lemmaRepository, IndexRepository indexRepository) {

        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {
        if (query.isBlank()) return SearchResponseGenerator.emptyQuery();
        LocalDateTime startTime = LocalDateTime.now();

        lock.lock();
        try {
            if (!query.equals(LastSearch.getQuery()) || !siteUrl.equals(LastSearch.getSite())) {
                LastSearch.clear();
                LastSearch.setQuery(query);
                LastSearch.setSite("");
                LastSearch.setResponse(siteUrl.equals("") ? globalSearch(query) : siteSearch(query, siteUrl));
            }
        } finally {
            lock.unlock();
        }


        SearchResponse response = new SearchResponse(LastSearch.getResponse());
        if (response.getData() != null) {
            response.setData(Arrays.stream(response.getData())
                    .skip(offset)
                    .limit(limit)
                    .toArray(SearchServiceResult[]::new));
        }

        LocalDateTime endTime = LocalDateTime.now();
        System.err.println(ChronoUnit.MILLIS.between(startTime, endTime));

        return response;
    }

    private SearchResponse commonSearch(String query, String siteUrl) {
        Site site = siteRepository.findByUrl(siteUrl);
        if (site == null || site.getStatus() == SiteStatus.INDEXING)
            return SearchResponseGenerator.siteNotIndexed();
        int siteId = site.getId();
        int pageCount = pageRepository.countBySiteId(siteId);
        Set<String> searchQueryWords = new HashSet<>(Lemmatizator.getLemmas(query).keySet());
        List<Lemma> lemmas = getQueryLemmasAscendingFrequency(query, siteId, pageCount);
        List<Page> pages = getPagesWithAllLemmas(lemmas);

        if (pages.isEmpty()) return SearchResponseGenerator.noResults();

        Map<Page, Set<Index>> pageAndIndexes = new HashMap<>();
        pages.forEach(page -> pageAndIndexes.put(page, indexRepository.findByPageIdAndLemmasId(page.getId(),
                lemmas.parallelStream()
                        .map(Lemma::getId)
                        .toList())));
        absRelevance.set(Math.max(absRelevance.get(), RelevanceWorker.getAbsRelevance(pageAndIndexes.values())));

        return SearchResponseGenerator
                .resultsFound(pages.size(), searchResults(searchQueryWords, pageAndIndexes));
    }

    private SearchResponse siteSearch(String query, String siteUrl) {
        SearchResponse response = commonSearch(query, siteUrl);

        if (!response.isResult()) return response;
        return calculateSearchResponsesRelevance(List.of(response)).get(0);
    }

    private SearchResponse globalSearch(String query) {
        List<Site> sites = siteRepository.findAll().stream()
                .filter(site -> site.getStatus() != SiteStatus.INDEXING)
                .toList();

        if (sites.isEmpty()) return SearchResponseGenerator.noSitesIndexed();

        List<SearchResponse> sitesResponses = sites.stream()
                .map(site -> commonSearch(query, site.getUrl()))
                .filter(SearchResponse::isResult)
                .toList();

        if (sitesResponses.isEmpty()) return SearchResponseGenerator.noResults();

        return SearchResponseGenerator
                .globalSearchResult(calculateSearchResponsesRelevance(sitesResponses));
    }

    private List<SearchResponse> calculateSearchResponsesRelevance(List<SearchResponse> sitesResponses) {
        sitesResponses.forEach(response ->
            Arrays.stream(response.getData()).forEach(result ->
                    result.setRelevance(RelevanceWorker.getRelRelevance(result.getRelevance(), absRelevance.get()))));

        return sitesResponses;
    }

    private List<Lemma> getQueryLemmasAscendingFrequency(String query, int siteId, int pageCount) {
        Set<String> lemmaValues = Lemmatizator.getLemmas(query).keySet();
        AtomicReference<Integer> expectedLemmasAmt = new AtomicReference<>(lemmaValues.size());
        List<Lemma> lemmas = lemmaValues.stream()
                .map(lemmaValue -> lemmaRepository.findBySiteIdAndLemma(siteId, lemmaValue))
                .filter(lemma -> lemma != null && filterTooFrequentLemmasOnSite(lemma, pageCount, expectedLemmasAmt))
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .toList();

        return lemmas.size() == expectedLemmasAmt.get() ? lemmas : new ArrayList<>();
    }

    private boolean filterTooFrequentLemmasOnSite(Lemma lemma, int pageCount, AtomicReference<Integer> expectedAmt) {
        if (pageCount < 100) return true;

        boolean res = (float) lemma.getFrequency() / pageCount < 0.05f;
        if (!res) expectedAmt.set(expectedAmt.get() - 1);
        return res;
    }

    private List<Page> getPagesWithAllLemmas(List<Lemma> lemmas) {
        if (lemmas.isEmpty()) return new ArrayList<>();
        if (lemmas.size() == 1) return lemmas.get(0).getLemmaPages().parallelStream().toList();
        return lemmas.get(0).getLemmaPages().stream()
                .filter(page -> page.getPageLemmas().containsAll(lemmas))
                .toList();
    }

    private SearchServiceResult[] searchResults(Set<String> words,
                                                Map<Page, Set<Index>> pageAndIndexes) {
        return pageAndIndexes.keySet().parallelStream()
                .map(page -> {
                    SearchServiceResult result = new SearchServiceResult();
                    result.setSite(page.getSite().getUrl());
                    result.setSiteName(page.getSite().getName());
                    result.setUri(page.getPath());
                    result.setTitle(HtmlWorker.getPageTitle(page.getContent()));
                    result.setSnippet(SnippetWorker.getSnippet(words, HtmlWorker.clearFromHtml(page.getContent())));
                    result.setRelevance((float) pageAndIndexes.get(page).parallelStream()
                            .mapToDouble(Index::getRank)
                            .sum());

                    return result;
                })
                .sorted(Comparator.comparingDouble(SearchServiceResult::getRelevance).reversed())
                .toArray(SearchServiceResult[]::new);
    }
}
