package searchengine.utils.parsers;

import org.jsoup.nodes.Document;
import searchengine.cache.PageCache;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.dto.page.PageResponse;
import searchengine.logging.ApplicationLogger;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.indexing.IndexingService;
import searchengine.utils.handlers.ParsingTaskResultHandler;
import searchengine.utils.lemmas.Lemmatizator;
import searchengine.utils.responsegenerators.IndexingResponseGenerator;
import searchengine.utils.workers.HtmlWorker;
import searchengine.utils.workers.UrlWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebsiteParser extends RecursiveTask<IndexingStatusResponse> {

    private static final int DELAY = 500;

    private static final AtomicBoolean parsingStopped;

    private final IndexingService indexingService;

    private final Site site;

    private String pageUrl;

    static {
        parsingStopped = new AtomicBoolean(true);
    }

    public WebsiteParser(IndexingService indexingService, Site site, String pageUrl) {
        this.indexingService = indexingService;
        this.site = site;
        this.pageUrl = pageUrl;
    }

    public static boolean isParsingStopped() {
        return parsingStopped.get();
    }

    public static void setParsingStopped(boolean toggle) {
        WebsiteParser.parsingStopped.set(toggle);
    }

    public Site getSite() {
        return site;
    }

    @Override
    protected IndexingStatusResponse compute() {
        if (parsingStopped.get()) return IndexingResponseGenerator.userStoppedIndexing();

        pageUrl = UrlWorker.removeDomainFromUrl(site.getUrl(), pageUrl);
        if (PageCache.pageIndexed(site.getId(), pageUrl)) return IndexingResponseGenerator.successResponse();

        executeDelay();

        Document doc = savePageInfoAndGetDocument();
        if (doc == null) return IndexingResponseGenerator.contentUnavailable(pageUrl);

        return new ParsingTaskResultHandler(new ArrayList<>(createSubtasks(doc))).handleTasksResult();
    }

    public IndexingStatusResponse indexPage() {
        Document doc = savePageInfoAndGetDocument();
        if (doc == null) return IndexingResponseGenerator.contentUnavailable(pageUrl);

        return IndexingResponseGenerator.successResponse();
    }

    protected Document savePageInfoAndGetDocument() {
        PageResponse pageResponse = HtmlWorker.getResponse(site.getUrl().concat(pageUrl));
        if (pageResponse == null || (pageResponse.getStatusCode() >= 400 && pageUrl.equals("/"))) return null;
        pageResponse.setPath(pageUrl);

        Page page = createPage(pageResponse);
        Document doc = HtmlWorker.parsePage(pageResponse.getResponse());
        Map<String, Integer> lemmasAndFrequencies = Lemmatizator.getLemmas(doc);

        if (pageResponse.getStatusCode() < 400) sendDataToIndexingService(site, page, lemmasAndFrequencies);

        return doc;
    }

    private Page createPage(PageResponse pageResponse) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(pageResponse.getPath());
        page.setCode(pageResponse.getStatusCode());
        page.setContent(page.getCode() >= 400 ? "" : pageResponse.getResponseBody());

        return page;
    }

    protected List<WebsiteParser> createSubtasks(Document doc) {

        List<WebsiteParser> subtasks = new ArrayList<>();

        for (String u : doc.select("a").eachAttr("abs:href")) {
            if (UrlWorker.isUrlValid(site.getUrl(), u)) {
                subtasks.add(new WebsiteParser(indexingService, site, UrlWorker.appendSlashToUrlEnd(u)));
            }
        }

        return subtasks;
    }

    protected void sendDataToIndexingService(Site site, Page page, Map<String, Integer> lemmasAndFrequencies) {
        indexingService.indexParsedData(site, page, lemmasAndFrequencies);
    }

    protected void executeDelay() {
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            ApplicationLogger.logError(e);
        }
    }
}
