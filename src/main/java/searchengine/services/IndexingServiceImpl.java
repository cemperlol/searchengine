package searchengine.services;

import lombok.NoArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.PageResponse;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Service
@ComponentScan
@NoArgsConstructor
public class IndexingServiceImpl
        extends RecursiveAction
        implements IndexingService {

    private static SiteService siteService;

    private static PageService pageService;

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
    public void startIndexing(List<searchengine.config.Site> siteList) {
        clearTablesBeforeStart();

        Set<IndexingServiceImpl> tasks = new HashSet<>();
        for (searchengine.config.Site configSite : siteList) {
            Site site = siteService.saveIndexingSite(configSite.getUrl(), configSite.getName());

            IndexingServiceImpl task =
                    new IndexingServiceImpl(site, site.getUrl());
            tasks.add(task);
        }

        ForkJoinPool pool = new ForkJoinPool();
        tasks.forEach(pool::invoke);
    }

    @Override
    public String stopIndexing() {
        //TODO: get tasks, shutdownNow those of them, which are running
        return "";
    }

    @Override
    protected void compute() {
        if (pageService.findByPathAndSiteId(pageUrl, site.getId()) != null) return;

        PageResponse pageResponse = HtmlService.getResponse(pageUrl);
        if (!savePage(pageResponse)) return;

        Document page = HtmlService.parsePage(pageResponse.getResponse());
//        if (page == null) return;

        site = siteService.updateSiteStatusTime(site.getId());
        executeDelay();
        createSubtasks(page).forEach(RecursiveAction::fork);
    }

    protected boolean savePage(PageResponse pageResponse) {
        pageResponse.setPath(pageUrl);
        pageService.savePage(pageResponse, site);

        return pageResponse.getResponse() != null;
    }

    protected Set<RecursiveAction> createSubtasks(Document doc) {
        Set<RecursiveAction> subtasks = Collections.synchronizedSet(new HashSet<>());
        Pattern sitePattern =
                Pattern.compile("://".concat(site.getUrl().substring(site.getUrl().indexOf(".") + 1)));

        doc.select("a").eachAttr("abs:href").forEach(u -> {
            if (sitePattern.matcher(u).find() && !u.contains("#"))
                subtasks.add(new IndexingServiceImpl(site, u));
        });

        return subtasks;
    }

    protected void executeDelay() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            ApplicationLogger.log(e);
        }
    }
}
