package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Service
public class IndexingServiceImpl
        extends RecursiveAction
        implements IndexingService {

    private SiteService siteService;

    private PageService pageService;

    private static List<Site> sitesList;

    @Autowired
    private void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    @Autowired
    private void setPageService(PageService pageService) {
        this.pageService = pageService;
    }

    @Autowired
    private void setSitesList(List<Site> sitesList) {
        IndexingServiceImpl.sitesList = sitesList;
    }
    
    @Override
    public void clearDatabaseBeforeStart() {
        siteService.deleteAllSites();
        pageService.deleteAllPages();
    }

    @Override
    public void startIndexing() {
        clearDatabaseBeforeStart();
        try (ForkJoinPool pool = new ForkJoinPool()) {
            pool.execute(this);
        }
    }

    @Override
    protected void compute() {

    }
}
