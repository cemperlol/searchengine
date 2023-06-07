package searchengine.utils.parsers;

import org.jsoup.nodes.Document;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.dto.page.PageResponse;
import searchengine.dto.parsing.ParsingTaskResult;
import searchengine.logging.ApplicationLogger;
import searchengine.model.Index;
import searchengine.model.Lemma;
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
import java.util.Set;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class WebsiteParser extends RecursiveTask<IndexingStatusResponse> {

    private static final int DELAY = 500;

    private static final AtomicBoolean parsingStopped;

    private static final List<ParsingSubscriber> subscribers;

    private final Site site;

    private String pageUrl;

    private final ParsingTaskResult result;

    static {
        parsingStopped = new AtomicBoolean(true);
        subscribers = new ArrayList<>();
    }

    public WebsiteParser(Site site, String pageUrl) {
        this.site = site;
        this.pageUrl = pageUrl;
        this.result = new ParsingTaskResult();
        result.setSite(site);
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
            IndexingResponseGenerator.successResponse();

        executeDelay();

        Document doc = savePageInfoAndGetDocument();
        if (doc == null) return IndexingResponseGenerator.contentUnavailable(pageUrl);

        notifySubscribers();

        return new ParsingTaskResultHandler(new ArrayList<>(createSubtasks(doc))).handleTasksResult();
    }

    protected Document savePageInfoAndGetDocument() {
        PageResponse pageResponse = HtmlWorker.getResponse(site.getUrl().concat(pageUrl));
        if (pageResponse == null) return null;
        pageResponse.setPath(pageUrl);

        Page page = createPage(pageResponse);

        result.setPage(page);
        if (page.getContent().equals("")) return null;

        Document doc = HtmlWorker.parsePage(pageResponse.getResponse());
        saveLemmasAndIndexes(page, doc);

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

    protected void saveLemmasAndIndexes(Page page, Document doc) {
        Map<String, Integer> lemmasAndFrequency = Lemmatizator.getLemmas(doc);

        List<Lemma> lemmas = createLemmas(page, lemmasAndFrequency);
        List<Integer> ranks = lemmasAndFrequency.values().stream().toList();

        result.setLemmas(lemmas);
        result.setIndexes(createIndexes(page, lemmas, ranks));
    }

    private List<Lemma> createLemmas(Page page, Map<String, Integer> lemmasAndFrequency) {
        Set<Lemma> pageLemmas = page.getPageLemmas();
        List<Lemma> lemmas = new ArrayList<>();
        lemmasAndFrequency.forEach((l, f) -> lemmas.add(createLemma(pageLemmas, l, f)));

        return lemmas;
    }

    private Lemma createLemma(Set<Lemma> pageLemmas, String lemmaValue, int frequency) {
        Lemma lemma = pageLemmas.stream()
                .filter(l -> l.getLemma().equals(lemmaValue))
                .findFirst()
                .orElse(null);

        if (lemma != null) {
            lemma.setFrequency(lemma.getFrequency() + frequency);
        } else {
            lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(lemmaValue);
            lemma.setFrequency(frequency);
        }

        return lemma;
    }

    private List<Index> createIndexes(Page page, List<Lemma> lemmas, List<Integer> ranks) {
        return IntStream.range(0, lemmas.size())
                .mapToObj(i -> createIndex(page, lemmas.get(i), ranks.get(i)))
                .toList();
    }

    private Index createIndex(Page page, Lemma lemma, int rank) {
        Index index = new Index();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);

        return index;
    }

    protected List<WebsiteParser> createSubtasks(Document doc) {
        Pattern sitePattern = Pattern.compile(site.getUrl());

        return doc.select("a").eachAttr("abs:href")
                .stream()
                .distinct()
                .filter(u -> sitePattern.matcher(HttpWorker.makeUrlWithoutWWW(u)).find()
                        && !u.contains("#") && !u.contains("?")
                        && site.getPages().stream().noneMatch(page -> page.getPath().equals(pageUrl)))
                .map(u -> new WebsiteParser(site, HttpWorker.makeUrlWithSlashEnd(u)))
                .toList();
    }

    protected void notifySubscribers() {
        subscribers.forEach(s -> s.update(result));
    }

    protected void executeDelay() {
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            ApplicationLogger.log(e);
        }
    }
}
