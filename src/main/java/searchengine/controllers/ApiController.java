package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.index.IndexService;
import searchengine.services.lemma.LemmaService;
import searchengine.services.page.PageService;
import searchengine.services.site.SiteService;
import searchengine.services.indexing.IndexingService;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.services.statistics.StatisticsService;
import searchengine.utils.IndexingResponseGenerator;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final SiteService siteService;

    private final PageService pageService;

    private final LemmaService lemmaService;

    private final IndexService indexService;

    private IndexingService indexator;

    public ApiController(StatisticsService statisticsService, SiteService siteService, PageService pageService,
                         LemmaService lemmaService, IndexService indexService) {
        this.statisticsService = statisticsService;
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
        if (indexator == null) return ResponseEntity.ok(IndexingResponseGenerator.failureNoIndexingRunning());

        IndexingToggleResponse response = indexator.stopIndexing();
        indexator = null;
        return ResponseEntity.ok(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingToggleResponse> indexPage(@RequestParam String url) {
        if (indexator == null) indexator = new IndexingServiceImpl(siteService, pageService, lemmaService, indexService);
        IndexingToggleResponse response = indexator.indexPage(url);

        return ResponseEntity.ok(response);
    }
}
