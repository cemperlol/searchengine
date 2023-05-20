package searchengine.services;

import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingResponseGenerator;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.dto.page.PageResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Service
@ComponentScan
@NoArgsConstructor
public class IndexingServiceImpl
        extends RecursiveTask<IndexingToggleResponse>
        implements IndexingService {

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
            IndexingServiceImpl task = new IndexingServiceImpl(s, s.getUrl().concat("/"));
            tasks.add(task);
            pool.submit(task);
        });

//        notifier.notifyIndexingStarted(IndexingResponseGenerator.successResponse());

        pool.shutdown();

        return IndexingResponseGenerator.successResponse();
//        return getTasksResult(tasks);
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
        site = siteService.findSiteByUrl(HtmlService.getBaseUrl(url));
        if (site == null) return IndexingResponseGenerator.failureSiteNotAdded();
        pageUrl = HtmlService.getUrlWithoutDomainName(site.getUrl(), HtmlService.makeUrlWithSlashEnd(url));

        clearTablesBeforeIndexPage();
        savePageInfoAndGetDocument();

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    protected IndexingToggleResponse compute() {
        pageUrl = HtmlService.getUrlWithoutDomainName(site.getUrl(), pageUrl);

        if (pageService.findByPathAndSiteId(pageUrl, site.getId()) != null)
            return IndexingResponseGenerator.successResponse();
        if (!pool.isShutdown()) executeDelay();

        Document doc = savePageInfoAndGetDocument();
        if (doc == null) return IndexingResponseGenerator.failurePageUnavailable(site.getUrl().concat(pageUrl));

        site = siteService.updateSiteStatusTime(site.getId());

        return getTasksResult(createSubtasks(doc));
    }

    private Document savePageInfoAndGetDocument() {
        PageResponse pageResponse = HtmlService.getResponse(site.getUrl().concat(pageUrl));
        if (pageResponse == null) return null;
        pageResponse.setPath(pageUrl);
        Page page = pageService.savePage(pageResponse, site);

        Document doc = HtmlService.parsePage(pageResponse.getResponse());

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
                .filter(u -> sitePattern.matcher(u).find() && !u.contains("#"))
                .map(u -> {
                    IndexingServiceImpl subtask = new IndexingServiceImpl(site, HtmlService.makeUrlWithoutSlashEnd(u).concat("/"));
                    subtask.fork();
                    return subtask;
                })
                .toList();
    }



    private IndexingToggleResponse getTasksResult(List<IndexingServiceImpl> tasks) {
        List<IndexingToggleResponse> results = new ArrayList<>();
        tasks.forEach(t -> results.add(t.join()));

        IndexingToggleResponse totalResult = results.stream()
                .filter(IndexingToggleResponse::isResult)
                .findFirst().orElse(null);

        if (totalResult == null) return IndexingResponseGenerator.successResponse();

        return IndexingResponseGenerator.createFailureResponse(totalResult.getError());
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
