package searchengine.services.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.LastSearch;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchServiceResult;
import searchengine.model.*;
import searchengine.utils.lemmas.Lemmatizator;
import searchengine.utils.responseGenerators.SearchResponseGenerator;
import searchengine.utils.workers.HtmlWorker;
import searchengine.utils.workers.RelevanceWorker;
import searchengine.utils.workers.SnippetWorker;

import java.util.*;

@Service
public class SearchServiceImpl implements SearchService {

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private float absRelevance = -1.0f;

    @Autowired
    public SearchServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                             LemmaRepository lemmaRepository) {

        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {
        if (query.isBlank()) return SearchResponseGenerator.emptyQuery();

        if (!query.equals(LastSearch.getQuery()) || !siteUrl.equals(LastSearch.getSite())) {
            LastSearch.clear();
            LastSearch.setQuery(query);
            LastSearch.setSite("");
            LastSearch.setResponse(siteUrl.equals("") ? globalSearch(query) : siteSearch(query, siteUrl));
        }

        SearchResponse response = new SearchResponse(LastSearch.getResponse());
        if (response.getData() != null) {
            response.setData(Arrays.stream(response.getData())
                    .skip(offset)
                    .limit(limit)
                    .toArray(SearchServiceResult[]::new));
        }

        return response;
    }

    private SearchResponse commonSearch(String query, String siteUrl) {
        Site site = siteRepository.findByUrl(siteUrl);
        if (site == null || site.getStatus() == SiteStatus.INDEXING)
            return SearchResponseGenerator.siteNotIndexed();
        int siteId = site.getId();
        int pageCount = pageRepository.countBySiteId(siteId);

        List<Lemma> lemmas = getAscendingLemmasFromQuery(query, siteId, pageCount);
        List<Page> pages = getPagesWithFullQuery(site, lemmas);

        if (pages.isEmpty()) return SearchResponseGenerator.noResults();

        Map<Page, Set<Index>> pageAndIndexes = new HashMap<>();
        pageCount = pages.size();
        pages.forEach(page -> pageAndIndexes.put(page, page.getIndexes()));
        absRelevance = Math.max(absRelevance, RelevanceWorker.getAbsRelevance(pageAndIndexes.values()));

        return SearchResponseGenerator
                .resultsFound(pageCount, searchResults(lemmas, pageAndIndexes));
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
                    result.setRelevance(RelevanceWorker.getRelRelevance(result.getRelevance(), absRelevance))));

        return sitesResponses;
    }

    private List<Lemma> getAscendingLemmasFromQuery(String query, int siteId, int pageCount) {
        Set<String> lemmaValues = Lemmatizator.getLemmas(query).keySet();
        List<Lemma> lemmas = lemmaValues.stream()
                .map(lemmaValue -> lemmaRepository.findBySiteIdAndLemma(siteId, lemmaValue))
                .filter(lemma -> lemma != null && filterTooFrequentLemmasOnSite(lemma, pageCount))
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .toList();

        return lemmas.size() == lemmaValues.size() ? lemmas : new ArrayList<>();
    }

    private boolean filterTooFrequentLemmasOnSite(Lemma lemma, int pageCount) {
        if (pageCount < 10) return true;
        return (float) lemma.getFrequency() / pageCount < 0.05f;
    }

    private List<Page> getPagesWithFullQuery(Site site, List<Lemma> lemmas) {
        if (lemmas.isEmpty()) return new ArrayList<>();
        if (lemmas.size() == 1) return lemmas.get(0).getLemmaPages().stream().toList();

        return site.getPages().stream()
                .filter(page -> page.getPageLemmas().containsAll(lemmas))
                .toList();
    }

    private SearchServiceResult[] searchResults(List<Lemma> lemmas,
                                                Map<Page, Set<Index>> pageAndIndexes) {
        List<SearchServiceResult> results = pageAndIndexes.keySet().stream()
                .map(page -> {
                    SearchServiceResult result = new SearchServiceResult();
                    result.setSite(page.getSite().getUrl());
                    result.setSiteName(page.getSite().getName());
                    result.setUri(page.getPath());
                    result.setTitle(HtmlWorker.getPageTitle(page.getContent()));
                    result.setSnippet(SnippetWorker.getSnippet(lemmas, HtmlWorker.clearFromHtml(page.getContent())));
                    result.setRelevance((float) pageAndIndexes.get(page).stream().mapToDouble(Index::getRank).sum());

                    return result;
                })
                .sorted(Comparator.comparingDouble(SearchServiceResult::getRelevance).reversed())
                .toList();

        return results.toArray(new SearchServiceResult[0]);
    }
}
