package searchengine.services;

import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.dto.page.PageResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
@ComponentScan
@NoArgsConstructor
public class IndexingServiceImpl
        extends RecursiveTask<IndexingToggleResponse>
        implements IndexingService {

    private static SiteService siteService;

    private static PageService pageService;

    private static ForkJoinPool pool;

    private Site site;

    private String pageUrl;

    private final AtomicReference<IndexingToggleResponse> lastResponse =
            new AtomicReference<>(new IndexingToggleResponse(false,
                    "Indexation have not started yet"));

    public IndexingServiceImpl(SiteService siteService, PageService pageService) {
        IndexingServiceImpl.siteService = siteService;
        IndexingServiceImpl.pageService = pageService;
    }

    private IndexingServiceImpl(Site site, String pageUrl) {
        this.site = site;
        this.pageUrl = pageUrl;
    }

    public void clearTablesBeforeStartIndexing() {
        IndexService.deleteAll();
        LemmaService.deleteAll();
        pageService.deleteAllPages();
        siteService.deleteAllSites();
    }

    public void clearTablesBeforeIndexPage() {
        int pageId = pageService.findByPathAndSiteId(pageUrl, site.getId()).getId();
        List<Lemma> lemmas = IndexService.getLemmasByPageId(pageId);
        IndexService.deleteIndexByPageId(pageId);
        LemmaService.deletePageInfo(lemmas);
        pageService.deletePageById(pageId);
    }

    @Override
    public IndexingToggleResponse startIndexing() {
        if (pool != null && !pool.isQuiescent())
            return new IndexingToggleResponse(false, "Indexing already started");
        clearTablesBeforeStartIndexing();
        pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

        for (searchengine.config.Site configSite : siteService.getSites()) {
            Site site = siteService.saveIndexingSite(configSite);
            IndexingServiceImpl task = new IndexingServiceImpl(site, site.getUrl().concat("/"));
            pool.submit(task);
        }

        return lastResponse.updateAndGet(r -> r = new IndexingToggleResponse(true));
    }

    @Override
    public IndexingToggleResponse stopIndexing() {
        if (pool == null) return lastResponse.updateAndGet(r ->
                r = new IndexingToggleResponse(false, "No indexing is running"));

        pool.shutdownNow();
        siteService.findAllSites().stream().filter(s -> !s.getStatus().equals(SiteStatus.INDEXED)).forEach(s -> {
            siteService.updateSiteLastError(s.getId(), "User stopped indexing");
            siteService.updateSiteStatus(s.getId(), SiteStatus.FAILED);
        });

        return lastResponse.updateAndGet(itr -> itr = new IndexingToggleResponse(true));
    }

    @Override
    public IndexingToggleResponse indexPage(String url) {
        site = siteService.findSiteByUrl(HtmlService.getBaseUrl(url));
        if (site == null)
            return new IndexingToggleResponse(false, "This website was not added to the site list");
        pageUrl = HtmlService
                .getUrlWithoutDomainName(site.getUrl(), HtmlService.makeUrlWithoutSlashEnd(url).concat("/"));

        clearTablesBeforeIndexPage();
        savePageAndGetDocument();

        return new IndexingToggleResponse(true);
    }

    @Override
    protected IndexingToggleResponse compute() {
        pageUrl = HtmlService.getUrlWithoutDomainName(site.getUrl(), pageUrl);
        if (pageService.findByPathAndSiteId(pageUrl, site.getId()) != null)
            return lastResponse.updateAndGet(itr -> itr = new IndexingToggleResponse(true));

        Document page = savePageAndGetDocument();
        site = siteService.updateSiteStatusTime(site.getId());
        if (!pool.isShutdown()) executeDelay();
        List<IndexingServiceImpl> subtasks = createSubtasks(page);
        List<IndexingToggleResponse> results = new ArrayList<>();
        subtasks.forEach(s -> results.add(s.invoke()));

        if (results.stream().filter(IndexingToggleResponse::isResult).findFirst().orElse(null) != null)
            return lastResponse.updateAndGet(itr -> itr = new IndexingToggleResponse(true));

        return lastResponse.updateAndGet(itr ->
                itr = new IndexingToggleResponse(false, "Indexing failed"));
    }

    protected Document savePageAndGetDocument() {
        PageResponse pageResponse = HtmlService.getResponse(site.getUrl().concat(pageUrl));
        pageResponse.setPath(pageUrl);
        Page page = pageService.savePage(pageResponse, site);
        Document doc = HtmlService.parsePage(pageResponse.getResponse());

        Map<String, Integer> lemmasAndFrequency = Lemmatizator.getLemmas(doc);
        LemmaService.saveAllLemmas(lemmasAndFrequency.keySet(), site)
                .forEach(lemma -> IndexService.saveIndex(page, lemma, lemmasAndFrequency.get(lemma.getLemma())));

        return doc;
    }

    protected List<IndexingServiceImpl> createSubtasks(Document doc) {
        List<IndexingServiceImpl> subtasks = Collections.synchronizedList(new ArrayList<>());
        Pattern sitePattern = Pattern.compile(site.getUrl());

        doc.select("a").eachAttr("abs:href").forEach(u -> {
            if (sitePattern.matcher(u).find() && !u.contains("#"))
                subtasks.add(new IndexingServiceImpl(site, HtmlService.makeUrlWithoutSlashEnd(u).concat("/")));
        });

        return subtasks;
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
