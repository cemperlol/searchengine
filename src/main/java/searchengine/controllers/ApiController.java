package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final SiteService siteService;

    private final PageService pageService;

    private IndexingService indexator;

    public ApiController(StatisticsService statisticsService, SiteService siteService, PageService pageService) {
        this.statisticsService = statisticsService;
        this.siteService = siteService;
        this.pageService = pageService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public String startIndexing() {
        indexator = new IndexingServiceImpl(siteService, pageService);
        indexator.startIndexing();

        return "";
    }

    @GetMapping("/stopIndexing")
    public String stopIndexing() {
        if (indexator == null) return "Indexation stop failed, because no indexator was found";

        indexator.stopIndexing();

        return "";
    }
}
