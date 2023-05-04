package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;

@Service
public class SiteService {

    private final SiteRepository siteRepository;

    @Autowired
    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }

    public void deleteAllSites() {
        siteRepository.deleteAll();
    }
}
