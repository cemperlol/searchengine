package searchengine.services;

import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.IndexingResult;
import searchengine.dto.statistics.PageResponse;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Service
@ComponentScan
@NoArgsConstructor
public class IndexingServiceImpl
        extends RecursiveTask<IndexingResult>
        implements IndexingService {

    private static SiteService siteService;

    private static PageService pageService;

    private static ForkJoinPool pool;

    private Site site;

    private String pageUrl;

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
    public List<IndexingResult> startIndexing() {
        clearTablesBeforeStart();

        Set<IndexingServiceImpl> tasks = new HashSet<>();
        for (searchengine.config.Site configSite : siteService.getSites()) {
            Site site = siteService.saveIndexingSite(configSite);
            IndexingServiceImpl task = new IndexingServiceImpl(site, site.getUrl());
            tasks.add(task);
        }

        pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        List<IndexingResult> results = new ArrayList<>();
        tasks.forEach(t -> results.add(pool.invoke(t)));

        results.forEach(r -> {
            if (!r.isIndexingSucceed()) {
                siteService.updateSiteLastError(r.getSiteId(), SiteStatus.FAILED, r.getErrorMessage());
            } else {
                siteService.updateSiteStatus(r.getSiteId(), SiteStatus.INDEXED);
            }
        });

        return results;
    }

    @Override
    public String stopIndexing() {
        if (pool == null) return "Indexation stop failed because no indexation is running";

        pool.shutdownNow();

        for (Site site : siteService.findAllSites()) {
            if (site.getStatus() == SiteStatus.INDEXED) {
                siteService.updateSiteStatus(site.getId(), SiteStatus.FAILED);
            }
        }

        return "Indexation stopped because of the user request";
    }

    @Override
    protected IndexingResult compute() {
        if (pool.isShutdown())
            return new IndexingResult(site.getId(), false, "User stopped indexing");

        if (pageService.findByPathAndSiteId(pageUrl, site.getId()) != null)
            return new IndexingResult(site.getId(), true);

        PageResponse pageResponse = HtmlService.getResponse(pageUrl);
        savePage(pageResponse);
        site = siteService.updateSiteStatusTime(site.getId());
        Document page = HtmlService.parsePage(pageResponse.getResponse());

        executeDelay();
        List<IndexingServiceImpl> subtasks = createSubtasks(page);
        subtasks.forEach(IndexingServiceImpl::invoke);

        /* TODO: add checker whether all IndexingResults've failed, if at least one have not, then indexation
            successful */

        return new IndexingResult(site.getId(), true);
    }

    protected void savePage(PageResponse pageResponse) {
        pageResponse.setPath(pageUrl);
        pageService.savePage(pageResponse, site);
    }

    protected List<IndexingServiceImpl> createSubtasks(Document doc) {
        List<IndexingServiceImpl> subtasks = Collections.synchronizedList(new ArrayList<>());
        Pattern sitePattern = Pattern.compile(site.getUrl().substring(site.getUrl().indexOf(".") + 1));
        Pattern httpsPattern = Pattern.compile("https?://");

        doc.select("a").eachAttr("abs:href").forEach(u -> {
            if (httpsPattern.matcher(u).find() && sitePattern.matcher(u).find() && !u.contains("#"))
                subtasks.add(new IndexingServiceImpl(site, HtmlService.makeUrlWithoutSlashEnd(u)));
        });

        return subtasks;
    }

    protected void executeDelay() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ApplicationLogger.log(e);
        }
    }
}
