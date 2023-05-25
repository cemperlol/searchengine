package searchengine.services.indexing;

import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.dto.page.PageResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.logging.ApplicationLogger;
import searchengine.services.index.IndexService;
import searchengine.services.lemma.LemmaService;
import searchengine.services.page.PageService;
import searchengine.services.site.SiteService;
import searchengine.utils.html.HtmlWorker;
import searchengine.utils.handlers.IndexingTaskResultHandler;
import searchengine.utils.lemmas.Lemmatizator;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Service
@NoArgsConstructor
public class IndexingServiceImpl
        extends AbstractIndexingService {

    private static SiteService siteService;

    private static PageService pageService;

    private static LemmaService lemmaService;

    private static IndexService indexService;

    private static ForkJoinPool pool;

    private Site site;

    private String pageUrl;

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

    private void clearTablesBeforeStartIndexing() {
        indexService.deleteAll();
        lemmaService.deleteAll();
        pageService.deleteAllPages();
        siteService.deleteAllSites();
    }

    private void clearTablesBeforeIndexPage() {
        int pageId = pageService.findByPathAndSiteId(pageUrl, site.getId()).getId();
        List<Lemma> lemmas = indexService.getLemmasByPageId(pageId);
        indexService.deleteIndexByPageId(pageId);
        lemmaService.deletePageInfo(lemmas);
        pageService.deletePageById(pageId);
    }

    @Override
    public IndexingToggleResponse startIndexing() {
        if (pool != null && !pool.isQuiescent()) return IndexingResponseGenerator.failureIndexingAlreadyStarted();
        clearTablesBeforeStartIndexing();
        pool = new ForkJoinPool();

        List<IndexingServiceImpl> tasks = new ArrayList<>();
        siteService.saveIndexingSites().forEach(s -> {
                    tasks.add(new IndexingServiceImpl(s, HtmlWorker.makeUrlWithSlashEnd(s.getUrl())));
                }

        );

        tasks.forEach(t -> CompletableFuture.runAsync(() -> processIndexingResult(t)));

        return IndexingResponseGenerator.successResponse();
    }

    private void processIndexingResult(IndexingServiceImpl task) {
        IndexingToggleResponse result = pool.invoke(task);

        if (result.isResult()) {
            siteService.saveSucceedIndexSite(task.site.getId());
        } else {
            siteService.saveFailedIndexSite(task.site.getId(), result.getError());
        }
    }

    @Override
    public IndexingToggleResponse stopIndexing() {
        if (pool == null) return IndexingResponseGenerator.failureNoIndexingRunning();

        pool.shutdownNow();
        siteService.updateSitesOnIndexingStop();

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    public IndexingToggleResponse indexPage(String url) {
        site = siteService.findSiteByUrl(HtmlWorker.getBaseUrl(url));
        if (site == null) return IndexingResponseGenerator.failureSiteNotAdded();
        pageUrl = HtmlWorker.getUrlWithoutDomainName(site.getUrl(), HtmlWorker.makeUrlWithSlashEnd(url));

        clearTablesBeforeIndexPage();
        savePageInfoAndGetDocument();

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    public IndexingToggleResponse compute() {
        pageUrl = HtmlWorker.getUrlWithoutDomainName(site.getUrl(), pageUrl);

        if (pageService.findByPathAndSiteId(pageUrl, site.getId()) != null)
            return IndexingResponseGenerator.successResponse();
        if (!pool.isShutdown()) executeDelay();

        Document doc = savePageInfoAndGetDocument();
        if (doc == null) return IndexingResponseGenerator.failurePageUnavailable(site.getUrl().concat(pageUrl));

        site = siteService.updateSiteStatusTime(site.getId());

        return new IndexingTaskResultHandler().HandleTasksResult(new ArrayList<>(createSubtasks(doc)));
    }

    private Document savePageInfoAndGetDocument() {
        PageResponse pageResponse = HtmlWorker.getResponse(site.getUrl().concat(pageUrl));
        if (pageResponse == null) return null;
        pageResponse.setPath(pageUrl);

        Page page = pageService.savePage(pageResponse, site);
        if (page == null || page.getContent().equals("Content is unknown")) return null;

        Document doc = HtmlWorker.parsePage(pageResponse.getResponse());

        saveLemmasAndIndexes(page, doc);

        return doc;
    }

    private void saveLemmasAndIndexes(Page page, Document doc) {
        Map<String, Integer> lemmasAndFrequency = Lemmatizator.getLemmas(doc);

        List<Lemma> lemmas = lemmaService.saveAllLemmas(lemmasAndFrequency.keySet(), site);
        List<Integer> ranks = lemmasAndFrequency.values().stream().toList();

        indexService.saveAllIndexes(page, lemmas, ranks);
    }

    protected List<IndexingServiceImpl> createSubtasks(Document doc) {
        Pattern sitePattern = Pattern.compile(site.getUrl());

        return doc.select("a").eachAttr("abs:href")
                .stream()
                .distinct()
                .filter(u -> sitePattern.matcher(HtmlWorker.makeUrlWithoutWWW(u)).find()
                        && !u.contains("#") && !u.contains("?")
                        && pageService.findByPathAndSiteId(u, site.getId()) == null)
                .map(u -> new IndexingServiceImpl(site, HtmlWorker.makeUrlWithSlashEnd(u)))
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