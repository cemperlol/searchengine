package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dao.index.IndexService;
import searchengine.dao.lemma.LemmaService;
import searchengine.dao.page.PageService;
import searchengine.dao.site.SiteService;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.dto.search.LastSearch;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchServiceResult;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dao.index.IndexServiceImpl;
import searchengine.services.indexing.IndexingService;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.dao.lemma.LemmaServiceImpl;
import searchengine.dao.page.PageServiceImpl;
import searchengine.services.search.SearchService;
import searchengine.services.search.SearchServiceImpl;
import searchengine.dao.site.SiteServiceImpl;
import searchengine.services.statistics.StatisticsService;
import searchengine.services.statistics.StatisticsServiceImpl;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;
import searchengine.utils.responseGenerators.SearchResponseGenerator;

import java.util.Arrays;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final SiteService siteServiceImpl;

    private final PageService pageServiceImpl;

    private final LemmaService lemmaServiceImpl;

    private final IndexService indexServiceImpl;

    private IndexingService indexator;

    @Autowired
    public ApiController(SiteServiceImpl siteServiceImpl, PageServiceImpl pageServiceImpl,
                         LemmaServiceImpl lemmaServiceImpl, IndexServiceImpl indexServiceImpl) {

        this.statisticsService = new StatisticsServiceImpl(siteServiceImpl);
        this.siteServiceImpl = siteServiceImpl;
        this.pageServiceImpl = pageServiceImpl;
        this.lemmaServiceImpl = lemmaServiceImpl;
        this.indexServiceImpl = indexServiceImpl;
    }



    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingToggleResponse> startIndexing() {
        indexator = new IndexingServiceImpl(siteServiceImpl, pageServiceImpl, lemmaServiceImpl, indexServiceImpl);
        IndexingToggleResponse response = indexator.startIndexing();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingToggleResponse> stopIndexing() {
        if (indexator == null) return ResponseEntity.ok(IndexingResponseGenerator.noIndexingRunning());

        IndexingToggleResponse response = indexator.stopIndexing();
        indexator = null;

        return ResponseEntity.ok(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingToggleResponse> indexPage(@RequestParam String url) {
        if (indexator == null)
            indexator = new IndexingServiceImpl(siteServiceImpl, pageServiceImpl,
                    lemmaServiceImpl, indexServiceImpl);
        IndexingToggleResponse response = indexator.indexPage(url);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam(name = "query") String query,
                                                 @RequestParam(name = "site", required = false, defaultValue = "")
                                                    String site,
                                                 @RequestParam(name = "offset", required = false) int offset,
                                                 @RequestParam(name = "limit", required = false) int limit) {

        if (query.isBlank()) return ResponseEntity.ok(SearchResponseGenerator.emptyQuery());

        if (!query.equals(LastSearch.getQuery()) || !site.equals(LastSearch.getSite())) {
            LastSearch.setQuery(query);
            LastSearch.setSite("");
            SearchService search = new SearchServiceImpl(siteServiceImpl, pageServiceImpl, lemmaServiceImpl);
            LastSearch.setResponse(site.equals("") ? search.globalSearch(query) : search.siteSearch(query, site));
        }

        SearchResponse response = new SearchResponse(LastSearch.getResponse());
        if (response.getData() == null) return ResponseEntity.ok(response);

        response.setData(Arrays.stream(response.getData())
                .skip(offset)
                .limit(limit)
                .toArray(SearchServiceResult[]::new));

        return ResponseEntity.ok(response);
    }
}
