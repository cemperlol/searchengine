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
        if (site == null || site.getStatus() == SiteStatus.INDEXING)
            return SearchResponseGenerator.siteNotIndexed();
        int pageCount = pageService.getTotalPageCount();

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
        sitesResponses.forEach(response ->
            Arrays.stream(response.getData()).forEach(result -> {
                Site site = siteService.findSiteByUrl(result.getSite());
                Set<Index> indexes = site.getPages().stream()
                        .filter(sitePage -> sitePage.getPath().equals(result.getUri()))
                        .findFirst().get().getIndexes();
                result.setRelevance(RelevanceWorker
                        .getRelRelevance(indexes, absRelevance));
            }));

        return sitesResponses;
    }

    private List<Lemma> getAscendingLemmasFromQuery(String query, Site site, int pageCount) {
        Set<String> lemmaValues = Lemmatizator.getLemmas(query).keySet();

        return site.getLemmas().stream()
                .filter(lemma -> lemmaValues.contains(lemma.getLemma()) &&
                        lemmaService.filterTooFrequentLemmasOnSite(lemma, pageCount))
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .toList();
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
