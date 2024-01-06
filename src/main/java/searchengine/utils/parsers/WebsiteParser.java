package searchengine.utils.parsers;

import org.jsoup.nodes.Document;
import searchengine.cache.PageCache;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.dto.page.PageResponse;
import searchengine.logging.ApplicationLogger;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.utils.data.DataReceiver;
import searchengine.utils.handlers.ParsingTaskResultHandler;
import searchengine.utils.lemmas.Lemmatizator;
import searchengine.utils.responsegenerators.IndexingResponseGenerator;
import searchengine.utils.workers.HtmlWorker;
import searchengine.utils.workers.HttpWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class WebsiteParser extends RecursiveTask<IndexingStatusResponse> {

    private static final int DELAY = 500;

    private static final AtomicBoolean parsingStopped;

    private final DataReceiver dataReceiver;

    private final Site site;

    private String pageUrl;

    static {
        parsingStopped = new AtomicBoolean(true);
    }

    public WebsiteParser(DataReceiver dataReceiver, Site site, String pageUrl) {
        this.dataReceiver = dataReceiver;
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

        pageUrl = HttpWorker.removeDomainFromUrl(site.getUrl(), pageUrl);
        if (PageCache.pageIndexed(site.getId(), pageUrl))
            return IndexingResponseGenerator.successResponse();

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
        if (pageResponse == null) return null;
        pageResponse.setPath(pageUrl);

        Page page = createPage(pageResponse);
        Document doc = null;
        Map<String, Integer> lemmasAndFrequencies = null;
        if (!page.getContent().equals("")) {
            doc = HtmlWorker.parsePage(pageResponse.getResponse());
            lemmasAndFrequencies = Lemmatizator.getLemmas(doc);
        }

        sendData(site, page, lemmasAndFrequencies);

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
        Pattern sitePattern = Pattern.compile(site.getUrl());
        List<WebsiteParser> subtasks = new ArrayList<>();

        for (String u : doc.select("a").eachAttr("abs:href")) {
            if (sitePattern.matcher(HttpWorker.removeWwwFromUrl(u)).find()
                    && !u.contains("#") && !u.contains("?")) {
                subtasks.add(new WebsiteParser(dataReceiver, site, HttpWorker.appendSlashToUrlEnd(u)));
            }
        }

        return subtasks;
    }

    protected void sendData(Site site, Page page, Map<String, Integer> lemmasAndFrequencies) {
        dataReceiver.receiveData(site, page, lemmasAndFrequencies);
    }

    protected void executeDelay() {
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            ApplicationLogger.logError(e);
        }
    }
}
