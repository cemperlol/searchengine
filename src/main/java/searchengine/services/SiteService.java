package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;

import java.sql.Timestamp;
import java.util.Optional;

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

    public Site saveIndexingSite(String url, String name) {
        Site site = new Site();
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));
        site.setUrl(url);
        site.setName(name);

        saveSite(site);

        return site;
    }

    public Site updateSiteStatusTime(int id) {
        siteRepository.updateSiteStatusTime(id);
        return siteRepository.findById(id).orElse(null);
    }

    public void deleteAllSites() {
        siteRepository.deleteAll();
    }
}
