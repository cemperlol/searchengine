package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IndexingServiceImpl implements IndexingService {

    private boolean indexingStatus;

    private SiteService siteService;

    private PageService pageService;

    private IndexingServiceImpl() {
        this.indexingStatus = false;
    }

    @Autowired
    private void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

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
    public boolean isIndexing() {
        return indexingStatus;
    }

    @Override
    public String startIndexing() {
        clearDatabaseBeforeStart();
        indexingStatus = true;

        

        return null;
    }
}
