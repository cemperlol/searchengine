package searchengine.utils.parsers;

import org.jsoup.nodes.Document;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.dto.page.PageResponse;
import searchengine.logging.ApplicationLogger;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.ParsingSubscriber;
import searchengine.utils.handlers.ParsingTaskResultHandler;
import searchengine.utils.lemmas.Lemmatizator;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;
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

    private static final List<ParsingSubscriber> subscribers;

    private final Site site;

    private String pageUrl;

    static {
        parsingStopped = new AtomicBoolean(true);
        subscribers = new ArrayList<>();
    }

    public WebsiteParser(Site site, String pageUrl) {
        this.site = site;
        this.pageUrl = pageUrl;
    }

    public static void subscribe(ParsingSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    public static void unsubscribe(ParsingSubscriber subscriber) {
        subscribers.remove(subscriber);
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

        pageUrl = HttpWorker.getUrlWithoutDomainName(site.getUrl(), pageUrl);
        if (site.getPages().stream().anyMatch(page -> page.getPath().equals(pageUrl)))
            return IndexingResponseGenerator.successResponse();

        executeDelay();

        Document doc = savePageInfoAndGetDocument();
        if (doc == null) return IndexingResponseGenerator.contentUnavailable(pageUrl);

        return new ParsingTaskResultHandler(new ArrayList<>(createSubtasks(doc))).handleTasksResult();
    }

    protected Document savePageInfoAndGetDocument() {
        PageResponse pageResponse = HtmlWorker.getResponse(site.getUrl().concat(pageUrl));
        if (pageResponse == null) return null;
        pageResponse.setPath(pageUrl);

        Page page = createPage(pageResponse);
        site.getPages().add(page);

        Document doc = null;
        Map<String, Integer> lemmasAndFrequency = null;
        if (!page.getContent().equals("")) {
            doc = HtmlWorker.parsePage(pageResponse.getResponse());
            lemmasAndFrequency = Lemmatizator.getLemmas(doc);
        }

        notifySubscribers(site, page, lemmasAndFrequency);

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
            if (sitePattern.matcher(HttpWorker.makeUrlWithoutWWW(u)).find()
                    && !u.contains("#") && !u.contains("?")) {
                subtasks.add(new WebsiteParser(site, HttpWorker.makeUrlWithSlashEnd(u)));
            }
        }

        return subtasks;
    }

    protected void notifySubscribers(Site site, Page page, Map<String, Integer> lemmasAndFrequency) {
        subscribers.forEach(s -> s.update(site, page, lemmasAndFrequency));
    }

    protected void executeDelay() {
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            ApplicationLogger.log(e);
        }
    }
}
