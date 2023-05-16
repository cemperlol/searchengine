package searchengine.services;

import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.dto.page.PageResponse;
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
            new AtomicReference<>(new IndexingToggleResponse(false, "Indexation have not started yet"));

    public IndexingServiceImpl(SiteService siteService, PageService pageService) {
        IndexingServiceImpl.siteService = siteService;
        IndexingServiceImpl.pageService = pageService;
    }

    private IndexingServiceImpl(Site site, String pageUrl) {
        this.site = site;
        this.pageUrl = pageUrl;
    }

    @Override
    public void clearTablesBeforeStart() {
        pageService.deleteAllPages();
        siteService.deleteAllSites();
    }

    @Override
    public IndexingToggleResponse startIndexing() {
        clearTablesBeforeStart();
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
        try {
            pool.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            ApplicationLogger.log(e);
        }
        for (Site site : siteService.findAllSites()) {
            if (site.getStatus() != SiteStatus.INDEXED) {
                siteService.updateSiteLastError(site.getId(), "User stopped indexing");
                siteService.updateSiteStatus(site.getId(), SiteStatus.FAILED);
            }
        }

        return lastResponse.updateAndGet(r -> r = new IndexingToggleResponse(true));
    }

    @Override
    public IndexingToggleResponse indexPage(String url) {
        if (siteService.findSiteByUrl(HtmlService.getBaseUrl(url)) == null)
            return new IndexingToggleResponse(false, "This website was not added into the site list");

        return new IndexingToggleResponse(true);
    }

    @Override
    protected IndexingToggleResponse compute() {
        pageUrl = HtmlService.getUrlWithoutDomainName(site.getUrl(), pageUrl);
        if (pageService.findByPathAndSiteId(pageUrl, site.getId()) != null)
            return lastResponse.updateAndGet(r -> r = new IndexingToggleResponse(true));

        PageResponse pageResponse = HtmlService.getResponse(site.getUrl().concat(pageUrl));
        savePage(pageResponse);

        if (pageResponse.getResponse().statusCode() != 200)
            siteService.updateSiteLastError(site.getId(), pageResponse.getCauseOfError());

        site = siteService.updateSiteStatusTime(site.getId());
        Document page = HtmlService.parsePage(pageResponse.getResponse());
        if (!pool.isShutdown()) executeDelay();
        List<IndexingServiceImpl> subtasks = createSubtasks(page);
        List<IndexingToggleResponse> results = new ArrayList<>();
        subtasks.forEach(s -> results.add(s.invoke()));

        if (results.stream().filter(IndexingToggleResponse::isResult).findFirst().orElse(null) != null) {
            return lastResponse.updateAndGet(r -> r = new IndexingToggleResponse(true));
        }
        return lastResponse.updateAndGet(r -> r = new IndexingToggleResponse(false, "Indexing failed"));
    }

    protected void savePage(PageResponse pageResponse) {
        pageResponse.setPath(pageUrl);
        pageService.savePage(pageResponse, site);
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
