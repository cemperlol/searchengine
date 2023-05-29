package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.dto.search.LastSearch;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchServiceResult;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.index.IndexService;
import searchengine.services.indexing.IndexingService;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.services.lemma.LemmaService;
import searchengine.services.page.PageService;
import searchengine.services.search.SearchService;
import searchengine.services.search.SearchServiceImpl;
import searchengine.services.site.SiteService;
import searchengine.services.statistics.StatisticsService;
import searchengine.services.statistics.StatisticsServiceImpl;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;
import searchengine.utils.responseGenerators.SearchResponseGenerator;

import java.util.Arrays;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final SiteService siteService;

    private final PageService pageService;

    private final LemmaService lemmaService;

    private final IndexService indexService;

    private IndexingService indexator;

    @Autowired
    public ApiController(SiteService siteService, PageService pageService,
                         LemmaService lemmaService, IndexService indexService) {
        this.statisticsService = new StatisticsServiceImpl(siteService, pageService, lemmaService);
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
    }



    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingToggleResponse> startIndexing() {
        indexator = new IndexingServiceImpl(siteService, pageService, lemmaService, indexService);
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
            indexator = new IndexingServiceImpl(siteService, pageService, lemmaService, indexService);
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
            SearchService search = new SearchServiceImpl(siteService, pageService, lemmaService, indexService);
            LastSearch.setResponse(site.equals("") ? search.globalSearch(query) : search.siteSearch(query, site));
        }

        SearchResponse response = new SearchResponse(LastSearch.getResponse());
        if (response.getData() == null) return ResponseEntity.ok(response);

        response.setData(Arrays.stream(response.getData()).skip(offset).limit(limit).toArray(SearchServiceResult[]::new));

        return ResponseEntity.ok(response);
    }
}
