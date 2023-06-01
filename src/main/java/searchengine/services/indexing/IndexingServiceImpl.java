package searchengine.services.indexing;

import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.services.index.IndexService;
import searchengine.services.lemma.LemmaService;
import searchengine.services.page.PageService;
import searchengine.services.site.SiteService;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.dto.page.PageResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.logging.ApplicationLogger;
import searchengine.utils.handlers.IndexingTaskResultHandler;
import searchengine.utils.lemmas.Lemmatizator;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;
import searchengine.utils.workers.HtmlWorker;
import searchengine.utils.workers.HttpWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Service
@NoArgsConstructor
public class IndexingServiceImpl extends RecursiveTask<IndexingToggleResponse>
        implements IndexingService {

    private static SiteService siteService;

    private static PageService pageService;

    private static LemmaService lemmaService;

    private static IndexService indexService;

    private static final ForkJoinPool POOL = new ForkJoinPool();

    private static final AtomicBoolean indexingStopped = new AtomicBoolean(true);

    private Site site;

    private String pageUrl;

    @Autowired
    public IndexingServiceImpl(SiteService siteService, PageService pageService,
                               LemmaService lemmaService, IndexService indexService) {
        IndexingServiceImpl.siteService = siteService;
        IndexingServiceImpl.pageService = pageService;
        IndexingServiceImpl.lemmaService = lemmaService;
        IndexingServiceImpl.indexService = indexService;
    }

    private IndexingServiceImpl(Site site, String pageUrl) {
        this.site = site;
        this.pageUrl = pageUrl;
    }

    @Override
    public boolean getIndexingStopped() {
        return indexingStopped.get();
    }

    private void clearTablesBeforeStartIndexing() {
        indexService.deleteAll();
        lemmaService.deleteAll();
        pageService.deleteAll();
        siteService.deleteAll();
    }

    private void clearTablesBeforeIndexPage() {
        int pageId = pageService.getByPathAndSiteId(pageUrl, site.getId()).getId();
        List<Lemma> lemmas = indexService.getLemmasByPageId(pageId);
        indexService.deleteByPageId(pageId);
        lemmaService.deletePageInfo(lemmas);
        pageService.deleteById(pageId);
    }

    @Override
    public IndexingToggleResponse startIndexing() {
        if (!indexingStopped.get()) return IndexingResponseGenerator.indexingAlreadyStarted();

        indexingStopped.set(false);
        CompletableFuture.runAsync(this::prepareIndexing);

        return IndexingResponseGenerator.successResponse();
    }

    private void prepareIndexing() {
        clearTablesBeforeStartIndexing();

        List<IndexingServiceImpl> tasks = siteService.getConfigSites().stream()
                .map(s -> new IndexingServiceImpl(siteService.save(s),
                        HttpWorker.makeUrlWithSlashEnd(s.getUrl())))
                .toList();

        tasks.forEach(t -> CompletableFuture.runAsync(() -> processIndexingResult(t)));
    }

    private void processIndexingResult(IndexingServiceImpl task) {
        IndexingToggleResponse result = POOL.invoke(task);

        if (result.isResult()) {
            siteService.updateStatus(task.site.getId(), SiteStatus.INDEXED);
        } else {
            siteService.updateLastError(task.site.getId(), result.getError());
        }

        if (POOL.getActiveThreadCount() == 0)
            indexingStopped.set(true);
    }

    @Override
    public IndexingToggleResponse stopIndexing() {
        if (indexingStopped.get()) return IndexingResponseGenerator.noIndexingRunning();

        indexingStopped.set(true);
        siteService.getAll()
                .forEach(site -> siteService.updateLastError(site.getId(), "User stopped indexing"));

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    public IndexingToggleResponse indexPage(String url) {
        site = siteService.getByUrl(HttpWorker.getBaseUrl(url));
        if (site == null) return IndexingResponseGenerator.siteNotAdded();
        pageUrl = HttpWorker.getUrlWithoutDomainName(site.getUrl(), HttpWorker.makeUrlWithSlashEnd(url));

        clearTablesBeforeIndexPage();
        savePageInfoAndGetDocument();

        site = siteService.updateStatusTime(site.getId());

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    public IndexingToggleResponse compute() {
        if (indexingStopped.get()) return IndexingResponseGenerator.userStoppedIndexing();

        pageUrl = HttpWorker.getUrlWithoutDomainName(site.getUrl(), pageUrl);

        if (pageService.getByPathAndSiteId(pageUrl, site.getId()) != null)
            return IndexingResponseGenerator.successResponse();

        executeDelay();

        Document doc = savePageInfoAndGetDocument();
        if (doc == null) return IndexingResponseGenerator.contentUnavailable(site.getUrl().concat(pageUrl));

        site = siteService.updateStatusTime(site.getId());

        return new IndexingTaskResultHandler(new ArrayList<>(createSubtasks(doc))).handleTasksResult();
    }

    protected Document savePageInfoAndGetDocument() {
        PageResponse pageResponse = HtmlWorker.getResponse(site.getUrl().concat(pageUrl));
        if (pageResponse == null) return null;
        pageResponse.setPath(pageUrl);

        Page page = pageService.save(pageResponse, site);

        if (page == null || page.getContent().equals("")) return null;

        Document doc = HtmlWorker.parsePage(pageResponse.getResponse());
        saveLemmasAndIndexes(page, doc);

        return doc;
    }

    protected void saveLemmasAndIndexes(Page page, Document doc) {
        Map<String, Integer> lemmasAndFrequency = Lemmatizator.getLemmas(doc);

        List<Lemma> lemmas = lemmaService.saveAll(lemmasAndFrequency.keySet(), site);
        List<Integer> ranks = lemmasAndFrequency.values().stream().toList();

        indexService.saveAll(page, lemmas, ranks);
    }

    protected List<IndexingServiceImpl> createSubtasks(Document doc) {
        Pattern sitePattern = Pattern.compile(site.getUrl());

        return doc.select("a").eachAttr("abs:href")
                .stream()
                .distinct()
                .filter(u -> sitePattern.matcher(HttpWorker.makeUrlWithoutWWW(u)).find()
                        && !u.contains("#") && !u.contains("?")
                        && pageService.getByPathAndSiteId(u, site.getId()) == null)
                .map(u -> new IndexingServiceImpl(site, HttpWorker.makeUrlWithSlashEnd(u)))
                .toList();
    }

    protected void executeDelay() {
        if (Thread.interrupted()) return;

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            ApplicationLogger.log(e);
        }
    }
}
