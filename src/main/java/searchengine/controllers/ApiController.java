package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.PageService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final SiteService siteService;

    private final PageService pageService;

    public ApiController(StatisticsService statisticsService, SiteService siteService, PageService pageService) {
        this.statisticsService = statisticsService;
        this.siteService = siteService;
        this.pageService = pageService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @Autowired
    public String startIndexing(SitesList sitesList) {
        IndexingServiceImpl indexator = new IndexingServiceImpl(siteService, pageService);

        indexator.startIndexing(sitesList.getSites());
        if (indexator.isCancelled()) return "Индексация не запущена";

        return "";
    }
}
