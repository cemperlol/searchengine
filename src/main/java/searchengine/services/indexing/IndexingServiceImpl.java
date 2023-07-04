package searchengine.services.indexing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.cache.PageCache;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.ParsingSubscriber;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.utils.parsers.WebsiteParser;
import searchengine.utils.responsegenerators.IndexingResponseGenerator;
import searchengine.utils.workers.HttpWorker;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class IndexingServiceImpl implements IndexingService, ParsingSubscriber {

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    private final SitesList configSites;

    private static final ForkJoinPool pool = new ForkJoinPool();

    private static final Lock lock = new ReentrantLock();

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
        CompletableFuture.runAsync(this::prepareForIndexing);

        return IndexingResponseGenerator.successResponse();
    }

    private void prepareForIndexing() {
        clearTablesBeforeStartIndexing();

        List<WebsiteParser> tasks = configSites.getSites().stream()
                .map(s -> {
                    Site site = new Site();
                    site.setStatus(SiteStatus.INDEXING);
                    site.setStatusTime(new Timestamp(System.currentTimeMillis()));
                    site.setUrl(HttpWorker.removeWwwFromUrl(s.getUrl()));
                    site.setName(s.getName());
                    siteRepository.save(site);

                    return new WebsiteParser(site, HttpWorker.appendSlashToUrlEnd(site.getUrl()));
                }).toList();

        tasks.forEach(t -> CompletableFuture.runAsync(() -> processIndexingResult(t)));
    }

    private void processIndexingResult(WebsiteParser task) {
        IndexingStatusResponse result = pool.invoke(task);

        int siteId = task.getSite().getId();
        if (result.isResult()) {
            siteRepository.updateStatus(siteId, SiteStatus.INDEXED);
        } else {
            siteRepository.updateLastError(siteId, result.getError());
        }
        PageCache.clearSitePagesCache(siteId);

        if (siteRepository.findAll().stream()
                .noneMatch(site -> site.getStatus().equals(SiteStatus.INDEXING))) {
            WebsiteParser.setParsingStopped(true);
            WebsiteParser.unsubscribe(this);
        }
    }

    @Override
    public IndexingStatusResponse stopIndexing() {
        if (WebsiteParser.isParsingStopped()) return IndexingResponseGenerator.noIndexingRunning();

        WebsiteParser.setParsingStopped(true);
        WebsiteParser.unsubscribe(this);
        PageCache.clearCache();
        siteRepository.findAll().stream()
                .filter(site -> site.getStatus().equals(SiteStatus.INDEXING))
                .forEach(site -> siteRepository.updateLastError(site.getId(), "User stopped indexing"));

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    public IndexingStatusResponse indexPage(String url) {
        url = url.trim();
        String baseUrl = HttpWorker.getBaseUrl(url);
        Site site = siteRepository.findByUrl(baseUrl);
        if (site == null && configSites.getSites().stream()
                        .noneMatch(s -> HttpWorker.removeWwwFromUrl(s.getUrl()).equals(baseUrl))) {
            return IndexingResponseGenerator.siteNotAdded();
        }
        if (site == null) {
            site = saveSite(baseUrl);
        }
        String pageUrl = HttpWorker.removeDomainFromUrl(site.getUrl(), HttpWorker.appendSlashToUrlEnd(url));

        clearTablesBeforeIndexPage(site, pageUrl);
        WebsiteParser task = new WebsiteParser(site, pageUrl);
        WebsiteParser.subscribe(this);

        IndexingStatusResponse response = task.indexPage();
        WebsiteParser.unsubscribe(this);

        return response;
    }

    private void clearTablesBeforeStartIndexing() {
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }

    private void clearTablesBeforeIndexPage(Site site, String pageUrl) {
        Page page = site.getPages().stream()
                .filter(sitePage -> sitePage.getPath().equals(pageUrl))
                .findFirst().orElse(null);
        if (page == null) return;

        indexRepository.deleteAllInBatch(page.getIndexes());
        List<Lemma> lemmasToRemove = new ArrayList<>();

        site.getLemmas().forEach(lemma -> {
            if (lemma.getFrequency() == 1) {
                lemmasToRemove.add(lemma);
                lemmaRepository.delete(lemma);
            } else {
                lemmaRepository.decrementFrequencyById(lemma.getId());
            }
        });
        lemmasToRemove.forEach(site.getLemmas()::remove);
        site.getPages().remove(page);
        pageRepository.delete(page);
    }

    private Site saveSite(String siteUrl) {
        Site site = new Site();
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));
        site.setUrl(HttpWorker.removeWwwFromUrl(siteUrl));
        site.setName(siteUrl);

        return siteRepository.save(site);
    }

    @Override
    public void update(Site site, Page page, Map<String, Integer> lemmasAndFrequencies) {
        int siteId = site.getId();

        lock.lock();
        try {
            if (PageCache.pageIndexed(siteId, page.getPath())) return;

            pageRepository.save(page);
            PageCache.addPageForSite(siteId, page.getPath());
        } finally {
            lock.unlock();
        }

        if (lemmasAndFrequencies != null) {
            lemmasAndFrequencies.keySet().forEach(lemmaValue -> lemmaRepository.save(siteId, lemmaValue));
            lemmasAndFrequencies.forEach((l, r) -> indexRepository.save(siteId, page.getPath(), l, r));
        }

        siteRepository.updateStatusTime(site.getId());
    }
}
