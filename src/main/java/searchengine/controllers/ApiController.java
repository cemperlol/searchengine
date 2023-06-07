package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final SearchService searchService;

    private final IndexingService indexingService;

    @Autowired
    public ApiController(IndexingService indexingService, StatisticsService statisticsService,
                         SearchService searchService) {
        this.indexingService = indexingService;
        this.statisticsService = statisticsService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingStatusResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingStatusResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingStatusResponse> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexingService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam(required = false, defaultValue = "") String site,
                                                 @RequestParam(required = false) int offset,
                                                 @RequestParam(required = false) int limit) {

        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}
