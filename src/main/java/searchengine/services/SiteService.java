package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;

import java.sql.Timestamp;
import java.util.List;

@Service
public class SiteService {

    private final SiteRepository siteRepository;

    private final SitesList sitesList;

    @Autowired
    public SiteService(SiteRepository siteRepository, SitesList sitesList) {
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
    }

    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }

    public Site saveIndexingSite(searchengine.config.Site configSite) {
        Site site = new Site();
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));
        site.setUrl(HtmlService
                .makeUrlWithoutSlashEnd(configSite.getUrl().replace("://www.", "://")));
        site.setName(configSite.getName());
        saveSite(site);

        return site;
    }

    public Site updateSiteStatusTime(int id) {
        siteRepository.updateSiteStatusTime(id);
        return siteRepository.findById(id).orElse(null);
    }

    public Site updateSiteStatus(int id, SiteStatus status) {
        siteRepository.updateSiteStatus(status, id);
        siteRepository.updateSiteStatusTime(id);
        return siteRepository.findById(id).orElse(null);
    }

    public Site updateSiteLastError(int id, String lastError) {
        siteRepository.updateSiteLastError(lastError, id);
        siteRepository.updateSiteStatusTime(id);
        return siteRepository.findById(id).orElse(null);
    }

    public Site findSiteById(int id) {
        return siteRepository.findById(id).orElse(null);
    }
    public List<Site> findAllSites() {
        return (List<Site>) siteRepository.findAll();
    }

    public void deleteAllSites() {
        siteRepository.deleteAll();
    }

    public List<searchengine.config.Site> getSites() {
        return sitesList.getSites();
    }
}
