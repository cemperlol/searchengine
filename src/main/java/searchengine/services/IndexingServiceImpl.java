package searchengine.services;

import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
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

    private String siteUrl;

    private String pageUrl;

    public IndexingServiceImpl(SiteService siteService, PageService pageService) {
        IndexingServiceImpl.siteService = siteService;
        IndexingServiceImpl.pageService = pageService;
    }

    private IndexingServiceImpl(String siteUrl, String pageUrl) {
        this.siteUrl = siteUrl;
        this.pageUrl = pageUrl;
    }

    @Override
    public void clearDatabaseBeforeStart() {
        siteService.deleteAllSites();
        pageService.deleteAllPages();
    }

    @Override
    public void startIndexing(List<searchengine.config.Site> sites) {
        clearDatabaseBeforeStart();

        List<IndexingServiceImpl> tasks = new ArrayList<>();
        for (searchengine.config.Site site : sites) {
            Site modelSite = new searchengine.model.Site();
            modelSite.setName(site.getName());
            modelSite.setStatus(SiteStatus.INDEXING);
            modelSite.setStatusTime(new Timestamp(System.currentTimeMillis()));
            modelSite.setUrl(site.getUrl());
            siteService.saveSite(modelSite);

            String siteUrl = site.getUrl();
            IndexingServiceImpl task =
                    new IndexingServiceImpl(siteUrl.substring(siteUrl.indexOf(".") + 1), site.getUrl());
            tasks.add(task);
        }

        ForkJoinPool pool = new ForkJoinPool();
        tasks.forEach(pool::invoke);
    }

    @Override
    protected void compute() {
        Document page = HtmlService.parsePage(pageUrl);
        if (page == null) return;

        executeDelay();
        createSubtasks(page).forEach(RecursiveAction::fork);
    }

    protected Set<RecursiveAction> createSubtasks(Document doc) {
        Set<RecursiveAction> subtasks = Collections.synchronizedSet(new HashSet<>());
        Pattern sitePattern = Pattern.compile(siteUrl);

        doc.select("a").eachAttr("abs:href").forEach(u -> {
            if (sitePattern.matcher(u).find() && !u.contains("#"))
                subtasks.add(new IndexingServiceImpl(siteUrl, u));
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
