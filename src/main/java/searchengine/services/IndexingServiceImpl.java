package searchengine.services;

import lombok.Getter;
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

    @Getter
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
        List<searchengine.config.Site> siteList = siteService.getSites();
        clearTablesBeforeStart();

        Set<IndexingServiceImpl> tasks = new HashSet<>();
        for (searchengine.config.Site configSite : siteList) {
            Site site = siteService.saveIndexingSite(configSite.getUrl(), configSite.getName());

            IndexingServiceImpl task =
                    new IndexingServiceImpl(site, site.getUrl());
            tasks.add(task);
        }

        pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        List<IndexingResult> results = new ArrayList<>();
        tasks.forEach(t -> results.add(pool.invoke(t)));

        results.forEach(r -> {
            if (!r.isIndexingSucceed()) {
                siteService.updateSiteStatus(r.getSiteId(), SiteStatus.FAILED);
            } else {
                siteService.updateSiteStatus(r.getSiteId(), SiteStatus.INDEXED);
            }
        });

        return results;
    }

    @Override
    public String stopIndexing() {
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
            return new IndexingResult(site.getId(),false, "User stopped indexing");

        if (pageService.findByPathAndSiteId(pageUrl, site.getId()) != null)
            return new IndexingResult(site.getId(), true);

        PageResponse pageResponse = HtmlService.getResponse(pageUrl);
        if (!savePage(pageResponse))
            return pageResponse.getStatusCode() == 200 ? new IndexingResult(site.getId(), true) :
                    new IndexingResult(site.getId(), false, pageResponse.getCauseOfError());

        Document page = HtmlService.parsePage(pageResponse.getResponse());
        site = siteService.updateSiteStatusTime(site.getId());

        executeDelay();
        List<IndexingServiceImpl> subtasks = createSubtasks(page);
        for (RecursiveTask<IndexingResult> subtask : subtasks) {
            subtask.invoke();
        }

        List<IndexingResult> results = new ArrayList<>();
        subtasks.forEach(s -> results.add(s.getRawResult()));

        return results.stream().filter(r -> !r.isIndexingSucceed()).findFirst()
                .orElse(new IndexingResult(site.getId(), true));
    }

    protected boolean savePage(PageResponse pageResponse) {
        pageResponse.setPath(pageUrl);
        pageService.savePage(pageResponse, site);

        return pageResponse.getResponse() != null;
    }

    protected List<IndexingServiceImpl> createSubtasks(Document doc) {
        List<IndexingServiceImpl> subtasks = Collections.synchronizedList(new ArrayList<>());
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
