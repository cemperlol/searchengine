package searchengine.services.search;

import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchServiceResult;
import searchengine.model.*;
import searchengine.services.index.IndexService;
import searchengine.services.lemma.LemmaService;
import searchengine.services.page.PageService;
import searchengine.services.site.SiteService;
import searchengine.utils.HtmlWorker;
import searchengine.utils.Lemmatizator;
import searchengine.utils.SearchResponseGenerator;

import java.util.*;

@Service
public class SearchServiceImpl implements SearchService {

    private final SiteService siteService;

    private final PageService pageService;

    private final LemmaService lemmaService;

    private final IndexService indexService;

    public SearchServiceImpl(SiteService siteService, PageService pageService,
                             LemmaService lemmaService, IndexService indexService) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
    }

    @Override
    public SearchResponse siteSearch(String query, String siteUrl, int offset, int limit) {
        Site site = siteService.findSiteByUrl(siteUrl);
        if (site == null || site.getStatus() == SiteStatus.INDEXING) return SearchResponseGenerator.siteNotIndexed();
        int pageCount = pageService.getPageCount(site.getId());

        List<Lemma> lemmas = getAscendingLemmasFromQuery(query, site.getId(), pageCount);
        List<Page> pages = getPagesWithFullQuery(lemmas, site.getId());

        if (pages.isEmpty()) return SearchResponseGenerator.noResults();

        Map<Page, List<Index>> pageQueryIndexes = new HashMap<>();
        pageCount = pages.size();
        pages.forEach(page -> pageQueryIndexes.put(page, indexService.getAllByLemmasId(lemmas)));

        return SearchResponseGenerator.resultsFound(pageCount, searchResults(lemmas, pageQueryIndexes));
    }

    @Override
    public SearchResponse globalSearch(String query, int offset, int limit) {
        return null;
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

    private SearchServiceResult[] searchResults(List<Lemma> lemmas, Map<Page, List<Index>> pageQueryIndexes) {
        List<SearchServiceResult> results = pageQueryIndexes.keySet().stream()
                .map(page -> {
                    SearchServiceResult result = new SearchServiceResult();
                    result.setSite(page.getSite().getUrl());
                    result.setSiteName(page.getSite().getName());
                    result.setUri(page.getPath());
                    result.setTitle(HtmlWorker.getPageTitle(page.getContent()));
                    result.setSnippet(HtmlWorker.getSnippet(lemmas, HtmlWorker.clearFromHtml(page.getContent())));
                    result.setRelevance(0);

                    return result;
                })
                .sorted(Comparator.comparingDouble(SearchServiceResult::getRelevance))
                .toList();

        return results.toArray(new SearchServiceResult[0]);
    }
}
