package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import searchengine.repositories.SiteRepository;

public class IndexingServiceImpl implements IndexingService {

    private boolean indexingStatus;

    private SiteService siteService;

    private PageService pageService;

    @Autowired
    public IndexingServiceImpl(SiteService siteService, PageService pageService) {
        this.indexingStatus = false;
        this.siteService = siteService;
        this.pageService = pageService;
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
