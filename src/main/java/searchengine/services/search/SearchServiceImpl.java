package searchengine.services.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.LastSearch;
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

    private float absRelevance = -1.0f;

    @Autowired
    public SearchServiceImpl(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {
        if (query.isBlank()) return SearchResponseGenerator.emptyQuery();

        if (!query.equals(LastSearch.getQuery()) || !siteUrl.equals(LastSearch.getSite())) {
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

    private SearchResponse siteSearch(String query, String siteUrl) {
        Site site = siteRepository.findByUrl(siteUrl).orElse(null);
        if (site == null || site.getStatus() == SiteStatus.INDEXING)
            return SearchResponseGenerator.siteNotIndexed();
        int pageCount = site.getPages().size();

        List<Lemma> lemmas = getAscendingLemmasFromQuery(query, site, pageCount);
        List<Page> pages = getPagesWithFullQuery(lemmas, site);

        if (pages.isEmpty()) return SearchResponseGenerator.noResults();

        Map<Page, Set<Index>> pageAndIndexes = new HashMap<>();
        pageCount = pages.size();
        pages.forEach(page -> pageAndIndexes.put(page, page.getIndexes()));
        absRelevance = Math.max(absRelevance, RelevanceWorker.getAbsRelevance(pageAndIndexes.values()));

        return SearchResponseGenerator
                .resultsFound(pageCount, searchResults(lemmas, pageAndIndexes, absRelevance));
    }

    private SearchResponse globalSearch(String query) {
        List<Site> sites = siteRepository.findAll().stream()
                .filter(site -> site.getStatus() != SiteStatus.INDEXING)
                .toList();

        if (sites.isEmpty()) return SearchResponseGenerator.noSitesIndexed();

        List<SearchResponse> sitesResponses = sites.stream()
                .map(site -> siteSearch(query, site.getUrl()))
                .filter(SearchResponse::isResult)
                .toList();

        if (sitesResponses.isEmpty()) return SearchResponseGenerator.noResults();

        return SearchResponseGenerator
                .globalSearchResult(calculateGlobalSearchResponsesRelevance(sitesResponses));
    }

    private List<SearchResponse> calculateGlobalSearchResponsesRelevance(List<SearchResponse> sitesResponses) {
        sitesResponses.forEach(response ->
            Arrays.stream(response.getData()).forEach(result -> {
                Site site = siteRepository.findByUrl(result.getSite()).get();
                Set<Index> indexes = site.getPages().stream()
                        .filter(sitesPage -> sitesPage.getPath().equals(result.getUri()))
                        .findFirst().get().getIndexes();
                result.setRelevance(RelevanceWorker.getRelRelevance(indexes, absRelevance));
            }));

        return sitesResponses;
    }

    private List<Lemma> getAscendingLemmasFromQuery(String query, Site site, int pageCount) {
        Set<String> lemmaValues = Lemmatizator.getLemmas(query).keySet();
        List<Lemma> lemmas = site.getLemmas().stream()
                .filter(lemma -> lemmaValues.contains(lemma.getLemma()) &&
                        filterTooFrequentLemmasOnSite(lemma, pageCount))
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .toList();

        return lemmas.size() == lemmaValues.size() ? lemmas : new ArrayList<>();
    }

    private boolean filterTooFrequentLemmasOnSite(Lemma lemma, int pageCount) {
        if (pageCount < 10) return true;
        return (float) lemma.getFrequency() / pageCount < 0.25;
    }

    private List<Page> getPagesWithFullQuery(List<Lemma> lemmas, Site site) {
        if (lemmas.isEmpty()) return new ArrayList<>();

        return site.getPages().stream()
                .filter(page -> page.getPageLemmas().containsAll(lemmas))
                .toList();
    }

    private SearchServiceResult[] searchResults(List<Lemma> lemmas,
                                                Map<Page, Set<Index>> pageAndIndexes,
                                                float absRelevance) {
        List<SearchServiceResult> results = pageAndIndexes.keySet().stream()
                .map(page -> {
                    SearchServiceResult result = new SearchServiceResult();
                    result.setSite(page.getSite().getUrl());
                    result.setSiteName(page.getSite().getName());
                    result.setUri(page.getPath());
                    result.setTitle(HtmlWorker.getPageTitle(page.getContent()));
                    result.setSnippet(SnippetWorker
                            .getSnippet(lemmas, HtmlWorker.clearFromHtml(page.getContent())));
                    result.setRelevance(RelevanceWorker.getRelRelevance(pageAndIndexes.get(page), absRelevance));

                    return result;
                })
                .sorted(Comparator.comparingDouble(SearchServiceResult::getRelevance).reversed())
                .toList();

        return results.toArray(new SearchServiceResult[0]);
    }
}
