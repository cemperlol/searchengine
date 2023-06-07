package searchengine.services.indexing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.parsing.ParsingTaskResult;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.ParsingSubscriber;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.utils.parsers.WebsiteParser;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;
import searchengine.utils.workers.HttpWorker;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

@Service
public class IndexingServiceImpl implements IndexingService, ParsingSubscriber {

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    private final SitesList configSites;

    private static final ForkJoinPool POOL = new ForkJoinPool();

    @Autowired
    public IndexingServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                               LemmaRepository lemmaRepository, IndexRepository indexRepository,
                               SitesList configSites) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.configSites = configSites;
    }

    @Override
    public IndexingStatusResponse startIndexing() {
        if (!WebsiteParser.isParsingStopped()) return IndexingResponseGenerator.indexingAlreadyStarted();

        WebsiteParser.setParsingStopped(false);
        WebsiteParser.subscribe(this);
        CompletableFuture.runAsync(this::prepareIndexing);

        return IndexingResponseGenerator.successResponse();
    }

    private void prepareIndexing() {
        clearTablesBeforeStartIndexing();

        List<WebsiteParser> tasks = configSites.getSites().stream()
                .map(s -> {
                    Site site = new Site();
                    site.setStatus(SiteStatus.INDEXING);
                    site.setStatusTime(new Timestamp(System.currentTimeMillis()));
                    site.setUrl(HttpWorker.makeUrlWithoutWWW(s.getUrl()));
                    site.setName(s.getName());
                    siteRepository.save(site);

                    return new WebsiteParser(site, HttpWorker.makeUrlWithSlashEnd(site.getUrl()));
                }).toList();

        tasks.forEach(t -> CompletableFuture.runAsync(() -> processIndexingResult(t)));
    }

    private void processIndexingResult(WebsiteParser task) {
        IndexingStatusResponse result = POOL.invoke(task);

        int siteId = task.getSite().getId();
        if (result.isResult()) {
            siteRepository.updateStatus(SiteStatus.INDEXED, siteId);
        } else {
            siteRepository.updateLastError(siteId, result.getError());
        }

        if (POOL.getActiveThreadCount() == 0 && POOL.isTerminating()) {
            WebsiteParser.setParsingStopped(true);
            WebsiteParser.unsubscribe(this);
        }
    }

    @Override
    public IndexingStatusResponse stopIndexing() {
        if (WebsiteParser.isParsingStopped()) return IndexingResponseGenerator.noIndexingRunning();

        WebsiteParser.setParsingStopped(true);
        WebsiteParser.unsubscribe(this);
        siteRepository.findAll().stream()
                .filter(site -> site.getStatus().equals(SiteStatus.INDEXING))
                .forEach(site -> siteRepository.updateLastError(site.getId(), "User stopped indexing"));

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    public IndexingStatusResponse indexPage(String url) {
        Site site = siteRepository.findByUrl(HttpWorker.getBaseUrl(url)).orElse(null);
        if (site == null) return IndexingResponseGenerator.siteNotAdded();

        CompletableFuture.runAsync(() -> processIndexPage(site, url));

        return IndexingResponseGenerator.successResponse();
    }

    private void processIndexPage(Site site, String url) {
        String pageUrl = HttpWorker.getUrlWithoutDomainName(site.getUrl(), HttpWorker.makeUrlWithSlashEnd(url));

        clearTablesBeforeIndexPage(site, pageUrl);


        siteRepository.updateStatusTime(site.getId());
    }

    private void clearTablesBeforeStartIndexing() {
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
    }

    private void clearTablesBeforeIndexPage(Site site, String pageUrl) {
        Page page = site.getPages().stream()
                .filter(sitePage -> sitePage.getPath().equals(pageUrl))
                .findFirst().orElse(null);
        if (page == null) return;

        int pageId = page.getId();
        Map<Lemma, Integer> lemmasAndFrequency = new HashMap<>();
        indexRepository.findByPageId(pageId)
                .forEach(i -> lemmasAndFrequency.put(i.getLemma(), (int) i.getRank()));

        indexRepository.deleteByPageId(pageId);
        lemmasAndFrequency.forEach((lemma, value) -> {
            if (lemma.getFrequency() == value) {
                lemmaRepository.delete(lemma);
            } else {
                lemmaRepository.updateFrequencyById(lemma.getId(), lemma.getFrequency() - value);
            }
        });
        pageRepository.deleteById(pageId);
    }

    @Override
    public void update(ParsingTaskResult result) {
        siteRepository.updateStatusTime(result.getSite().getId());
        pageRepository.save(result.getPage());
        result.getLemmas().forEach(lemmaRepository::safeSave);
        result.getIndexes().forEach(indexRepository::safeSave);
    }
}
