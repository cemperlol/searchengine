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
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.utils.parsers.WebsiteParser;
import searchengine.utils.responsegenerators.IndexingResponseGenerator;
import searchengine.utils.workers.UrlWorker;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    private final SitesList configSites;

    private static ForkJoinPool pool;

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

        PageCache.clearCache();
        pool = new ForkJoinPool();
        WebsiteParser.setParsingStopped(false);
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
                    site.setUrl(UrlWorker.removeWwwFromUrl(s.getUrl()));
                    site.setName(s.getName());
                    siteRepository.save(site);

                    return new WebsiteParser(this, site, UrlWorker.appendSlashToUrlEnd(site.getUrl()));
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
            pool.shutdown();
            WebsiteParser.setParsingStopped(true);
        }
    }

    @Override
    public IndexingStatusResponse stopIndexing() {
        if (WebsiteParser.isParsingStopped()) return IndexingResponseGenerator.noIndexingRunning();

        WebsiteParser.setParsingStopped(true);
        pool.shutdown();
        PageCache.clearCache();
        siteRepository.findAll().stream()
                .filter(site -> site.getStatus().equals(SiteStatus.INDEXING))
                .forEach(site -> siteRepository.updateLastError(site.getId(), "User stopped indexing"));

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    public IndexingStatusResponse indexPage(String url) {
        url = url.trim();
        String baseUrl = UrlWorker.getBaseUrl(url);
        Site site = findSiteToIndexPage(baseUrl);
        if (site == null) return IndexingResponseGenerator.siteNotAdded();
        String pageUrl = UrlWorker.removeDomainFromUrl(site.getUrl(), UrlWorker.appendSlashToUrlEnd(url));

        clearTablesBeforeIndexPage(site, pageUrl);
        WebsiteParser.setParsingStopped(false);
        CompletableFuture.runAsync(() -> {
            WebsiteParser task = new WebsiteParser(this, site, pageUrl);
            task.indexPage();
            WebsiteParser.setParsingStopped(true);
        });

        return IndexingResponseGenerator.successResponse();
    }

    private Site findSiteToIndexPage(String baseUrl) {
        Site site = siteRepository.findByUrl(baseUrl);
        if (site == null) {
            if (configSites.getSites().stream()
                    .noneMatch(s -> UrlWorker.removeWwwFromUrl(s.getUrl()).equals(baseUrl))) {
                return null;
            }
            site = saveSite(baseUrl);
        }

        return site;
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

        List<Lemma> lemmasToRemove = new ArrayList<>();
        Set<Lemma> pageLemmas = page.getPageLemmas();
        indexRepository.deleteAllInBatch(page.getIndexes());
        for (Lemma lemma : pageLemmas) {
            if (lemma.getFrequency() == 1) {
                lemmasToRemove.add(lemma);
            } else {
                lemmaRepository.decrementFrequencyById(lemma.getId());
            }
        }
        lemmasToRemove.forEach(site.getLemmas()::remove);
        lemmaRepository.deleteAllInBatch(lemmasToRemove);
        site.getPages().remove(page);
        pageRepository.delete(page);
    }

    private Site saveSite(String siteUrl) {
        Site site = new Site();
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));
        site.setUrl(UrlWorker.removeWwwFromUrl(siteUrl));
        site.setName(siteUrl);

        return siteRepository.save(site);
    }

    @Override
    public void indexParsedData(Site site, Page page, Map<String, Integer> lemmasAndFrequencies) {
        if (WebsiteParser.isParsingStopped()) return;
        int siteId = site.getId();

        lock.lock();
        try {
            if (PageCache.pageIndexed(siteId, page.getPath())) return;

            pageRepository.save(page);
            PageCache.addPageForSite(siteId, page.getPath());
        } finally {
            lock.unlock();
        }

        lemmasAndFrequencies.forEach((l, f) -> {
            lemmaRepository.save(siteId, l);
            indexRepository.save(siteId, page.getPath(), l, f);
        });

        siteRepository.updateStatusTime(site.getId());
    }
}
