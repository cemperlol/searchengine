package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
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
    public ResponseEntity<IndexingToggleResponse> startIndexing() {
        indexator = new IndexingServiceImpl(siteService, pageService);
        IndexingToggleResponse response = indexator.startIndexing();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingToggleResponse> stopIndexing() {
        if (indexator == null)
            return ResponseEntity.ok(new IndexingToggleResponse(false, "No indexing is running"));

        IndexingToggleResponse response = indexator.stopIndexing();
        indexator = null;
        return ResponseEntity.ok(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingToggleResponse> indexPage(@RequestParam String url) {
        indexator = new IndexingServiceImpl(siteService, pageService);
        IndexingToggleResponse response = indexator.indexPage(url);

        return ResponseEntity.ok(response);
    }

    @Autowired
    @GetMapping
    public void configureLemmatizator(LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        LemmaService.setLemmaRepository(lemmaRepository);
        IndexService.setIndexRepository(indexRepository);
    }
}
