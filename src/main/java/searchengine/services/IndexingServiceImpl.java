package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;

import java.util.concurrent.RecursiveAction;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl
        extends RecursiveAction
        implements IndexingService {

    private SiteService siteService;

    private PageService pageService;

    private final SitesList sites;

    @Autowired
    private void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    @Autowired
    private void setPageService(PageService pageService) {
        this.pageService = pageService;
    }

    private static final class InstanceHolder {
        private static final IndexingServiceImpl INSTANCE = new IndexingServiceImpl();
    }

    public static IndexingServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public void clearDatabaseBeforeStart() {
        siteService.deleteAllSites();
        pageService.deleteAllPages();
    }

    @Override
    public void startIndexing() {
        clearDatabaseBeforeStart();

        this.compute();
    }

    @Override
    protected void compute() {

    }
}
