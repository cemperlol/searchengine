package searchengine.services.indexing;

import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dao.index.IndexService;
import searchengine.dao.lemma.LemmaService;
import searchengine.dao.page.PageService;
import searchengine.dao.site.SiteService;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.dto.page.PageResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.services.logging.ApplicationLogger;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Service
@NoArgsConstructor
public class IndexingServiceImpl
        extends AbstractIndexingService {

    private static SiteService siteServiceImpl;

    private static PageService pageServiceImpl;

    private static LemmaService lemmaServiceImpl;

    private static IndexService indexServiceImpl;

    private static ForkJoinPool pool;

    private static final AtomicBoolean indexingStopped = new AtomicBoolean();

    private Site site;

    private String pageUrl;

    public IndexingServiceImpl(SiteService siteServiceImpl, PageService pageServiceImpl,
                               LemmaService lemmaServiceImpl, IndexService indexServiceImpl) {
        IndexingServiceImpl.siteServiceImpl = siteServiceImpl;
        IndexingServiceImpl.pageServiceImpl = pageServiceImpl;
        IndexingServiceImpl.lemmaServiceImpl = lemmaServiceImpl;
        IndexingServiceImpl.indexServiceImpl = indexServiceImpl;
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
        indexServiceImpl.deleteAll();
        lemmaServiceImpl.deleteAll();
        pageServiceImpl.deleteAll();
        siteServiceImpl.deleteAll();
    }

    private void clearTablesBeforeIndexPage() {
        int pageId = pageServiceImpl.getByPathAndSiteId(pageUrl, site.getId()).getId();
        List<Lemma> lemmas = indexServiceImpl.getLemmasByPageId(pageId);
        indexServiceImpl.deleteByPageId(pageId);
        lemmaServiceImpl.deletePageInfo(lemmas);
        pageServiceImpl.deleteById(pageId);
    }

    @Override
    public IndexingToggleResponse startIndexing() {
        if (pool != null && !indexingStopped.get()) return IndexingResponseGenerator.indexingAlreadyStarted();
        indexingStopped.set(false);
        pool = new ForkJoinPool();

        CompletableFuture.runAsync(this::prepareIndexing);

        return IndexingResponseGenerator.successResponse();
    }

    private void prepareIndexing() {
        clearTablesBeforeStartIndexing();

        List<IndexingServiceImpl> tasks = siteServiceImpl.getConfigSites().stream()
                .map(s -> new IndexingServiceImpl(siteServiceImpl.save(s),
                        HttpWorker.makeUrlWithSlashEnd(s.getUrl())))
                .toList();

        tasks.forEach(t -> CompletableFuture.runAsync(() -> processIndexingResult(t)));
    }

    private void processIndexingResult(IndexingServiceImpl task) {
        IndexingToggleResponse result = pool.invoke(task);

        if (result.isResult()) {
            siteServiceImpl.updateStatus(task.site.getId(), SiteStatus.INDEXED);
        } else {
            siteServiceImpl.updateLastError(task.site.getId(), result.getError());
        }
    }

    @Override
    public IndexingToggleResponse stopIndexing() {
        if (indexingStopped.get()) return IndexingResponseGenerator.noIndexingRunning();

        indexingStopped.set(true);
        siteServiceImpl.getAll()
                .forEach(site -> siteServiceImpl.updateLastError(site.getId(), "User stopped indexing"));

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    public IndexingToggleResponse indexPage(String url) {
        site = siteServiceImpl.getByUrl(HttpWorker.getBaseUrl(url));
        if (site == null) return IndexingResponseGenerator.siteNotAdded();
        pageUrl = HttpWorker.getUrlWithoutDomainName(site.getUrl(), HttpWorker.makeUrlWithSlashEnd(url));

        clearTablesBeforeIndexPage();
        savePageInfoAndGetDocument();

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    public IndexingToggleResponse compute() {
        if (indexingStopped.get()) return IndexingResponseGenerator.userStoppedIndexing();

        pageUrl = HttpWorker.getUrlWithoutDomainName(site.getUrl(), pageUrl);

        if (pageServiceImpl.getByPathAndSiteId(pageUrl, site.getId()) != null)
            return IndexingResponseGenerator.successResponse();

        executeDelay();

        Document doc = savePageInfoAndGetDocument();
        if (doc == null) return IndexingResponseGenerator.contentUnavailable(site.getUrl().concat(pageUrl));
        if (doc.baseUri().equals(pageUrl)) return IndexingResponseGenerator.successResponse();

        site = siteServiceImpl.updateStatusTime(site.getId());

        return new IndexingTaskResultHandler(new ArrayList<>(createSubtasks(doc))).HandleTasksResult();
    }

    protected Document savePageInfoAndGetDocument() {
        PageResponse pageResponse = HtmlWorker.getResponse(site.getUrl().concat(pageUrl));
        if (pageResponse == null) return null;
        pageResponse.setPath(pageUrl);

        Page page = pageServiceImpl.save(pageResponse, site);
        if (page == null) return null;
        if (page.getContent().equals("Content is unknown")) return new Document(pageUrl);

        Document doc = HtmlWorker.parsePage(pageResponse.getResponse());

        saveLemmasAndIndexes(page, doc);

        return doc;
    }

    protected void saveLemmasAndIndexes(Page page, Document doc) {
        Map<String, Integer> lemmasAndFrequency = Lemmatizator.getLemmas(doc);

        List<Lemma> lemmas = lemmaServiceImpl.saveAll(lemmasAndFrequency.keySet(), site);
        List<Integer> ranks = lemmasAndFrequency.values().stream().toList();

        indexServiceImpl.saveAll(page, lemmas, ranks);
    }

    protected List<IndexingServiceImpl> createSubtasks(Document doc) {
        Pattern sitePattern = Pattern.compile(site.getUrl());

        return doc.select("a").eachAttr("abs:href")
                .stream()
                .distinct()
                .filter(u -> sitePattern.matcher(HttpWorker.makeUrlWithoutWWW(u)).find()
                        && !u.contains("#") && !u.contains("?")
                        && pageServiceImpl.getByPathAndSiteId(u, site.getId()) == null)
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
