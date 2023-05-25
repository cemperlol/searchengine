package searchengine.services.search;

import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchServiceResult;
import searchengine.model.*;
import searchengine.services.index.IndexService;
import searchengine.services.lemma.LemmaService;
import searchengine.services.page.PageService;
import searchengine.services.site.SiteService;
import searchengine.utils.lemmas.Lemmatizator;
import searchengine.utils.responseGenerators.SearchResponseGenerator;
import searchengine.utils.workers.HtmlWorker;
import searchengine.utils.workers.RelevanceWorker;
import searchengine.utils.workers.SnippetWorker;

import java.util.*;

@Service
public class SearchServiceImpl implements SearchService {

    private final SiteService siteService;

    private final PageService pageService;

    private final LemmaService lemmaService;

    private final IndexService indexService;

    private float absRelevance = -1.0f;

    public SearchServiceImpl(SiteService siteService, PageService pageService,
                             LemmaService lemmaService, IndexService indexService) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
    }

    @Override
    public SearchResponse siteSearch(String query, String siteUrl) {
        Site site = siteService.findSiteByUrl(siteUrl);
        if (site == null || site.getStatus() == SiteStatus.INDEXING) return SearchResponseGenerator.siteNotIndexed();
        int pageCount = pageService.getPageCount(site.getId());

        List<Lemma> lemmas = getAscendingLemmasFromQuery(query, site.getId(), pageCount);
        List<Page> pages = getPagesWithFullQuery(lemmas, site.getId());

        if (pages.isEmpty()) return SearchResponseGenerator.noResults();

        Map<Page, List<Index>> pageAndIndexes = new HashMap<>();
        pageCount = pages.size();
        pages.forEach(page -> pageAndIndexes.put(page, indexService.getByPageId(page.getId())));
        absRelevance = Math.max(absRelevance, RelevanceWorker.getAbsRelevance(pageAndIndexes.values()));

        return SearchResponseGenerator.resultsFound(pageCount, searchResults(lemmas, pageAndIndexes, absRelevance));
    }

    @Override
    public SearchResponse globalSearch(String query) {
        List<Site> sites = siteService.findAllSites().stream()
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
        sitesResponses.forEach(response -> {
            Arrays.stream(response.getData()).forEach(result -> {
                Page page = pageService.findByPathAndSiteId(result.getUri(),
                        siteService.findSiteByUrl(result.getSite()).getId());

                result.setRelevance(RelevanceWorker
                        .getRelRelevance(indexService.getByPageId(page.getId()), absRelevance));
            });
        });

        return sitesResponses;
    }

    private List<Lemma> getAscendingLemmasFromQuery(String query, int siteId, int pageCount) {
        return Lemmatizator.getLemmas(query).keySet().stream()
                .map(lemmaValue -> lemmaService.getByLemmaAndSiteId(lemmaValue, siteId))
                .filter(Objects::nonNull)
                .filter(lemma -> lemmaService.filterTooFrequentLemmasOnSite(lemma, pageCount))
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .toList();
    }

    private List<Page> getPagesWithFullQuery(List<Lemma> lemmas, int siteId) {
        if (lemmas.isEmpty()) return new ArrayList<>();

        return indexService.getPagesByLemmaId(lemmas.get(0).getId()).stream()
                .filter(page -> lemmas.stream()
                        .allMatch(lemma -> indexService.pageContainsLemma(lemma.getId(), page.getId())))
                .filter(page -> page.getSite().getId() == siteId)
                .distinct()
                .toList();
    }

    private SearchServiceResult[] searchResults(List<Lemma> lemmas,
                                                Map<Page, List<Index>> pageAndIndexes,
                                                float absRelevance) {
        List<SearchServiceResult> results = pageAndIndexes.keySet().stream()
                .map(page -> {
                    SearchServiceResult result = new SearchServiceResult();
                    result.setSite(page.getSite().getUrl());
                    result.setSiteName(page.getSite().getName());
                    result.setUri(page.getPath());
                    result.setTitle(HtmlWorker.getPageTitle(page.getContent()));
                    result.setSnippet(SnippetWorker.getSnippet(lemmas, HtmlWorker.clearFromHtml(page.getContent())));
                    result.setRelevance(RelevanceWorker.getRelRelevance(pageAndIndexes.get(page), absRelevance));

                    return result;
                })
                .sorted(Comparator.comparingDouble(SearchServiceResult::getRelevance).reversed())
                .toList();

        return results.toArray(new SearchServiceResult[0]);
    }
}
