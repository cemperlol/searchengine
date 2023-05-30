package searchengine.dao.site;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;
import searchengine.utils.workers.HtmlWorker;
import searchengine.utils.workers.HttpWorker;

import java.sql.Timestamp;
import java.util.List;

@Service
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;

    private final SitesList sitesList;

    @Autowired
    public SiteServiceImpl(SiteRepository siteRepository, SitesList sitesList) {
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
    }

    @Override
    public Site save(searchengine.config.Site configSite) {
        Site site = siteRepository.findByUrl(configSite.getUrl()).orElse(null);

        if (site == null) {
            site = new Site();
            site.setStatus(SiteStatus.INDEXING);
            site.setUrl(HttpWorker.makeUrlWithoutSlashEnd(HttpWorker.makeUrlWithoutWWW(configSite.getUrl())));
            site.setName(configSite.getName());
        }
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));

        return siteRepository.save(site);
    }

    @Override
    public Site getByUrl(String url) {
        return siteRepository.findByUrl(url.replace("://www.", "://")).orElse(null);
    }

    @Override
    public List<Site> getAll() {
        return (List<Site>) siteRepository.findAll();
    }

    @Override
    public int getTotalCount() {
        return siteRepository.totalCount();
    }

    @Override
    public Site updateStatus(int id, SiteStatus status) {
        siteRepository.updateStatus(status, id);

        return updateStatusTime(id);
    }

    @Override
    public Site updateIncorrectShutdown(Site site) {
        return site.getStatus().equals(SiteStatus.INDEXING)
                ? updateLastError(site.getId(),"Application was shut down while indexing")
                : site;
    }

    @Override
    public Site updateStatusTime(int id) {
        siteRepository.updateStatusTime(id);

        return siteRepository.findById(id).orElse(null);
    }

    @Override
    public Site updateLastError(int id, String lastError) {
        siteRepository.updateLastError(lastError, id);
        updateStatus(id, SiteStatus.FAILED);

        return updateStatus(id, SiteStatus.FAILED);
    }

    @Override
    public void deleteById(int id) {
        siteRepository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        int batchSize = 500;
        int totalRowsAffected = 0;

        do {
            siteRepository.deleteAllInBatches(batchSize);
            totalRowsAffected += batchSize;
        } while (totalRowsAffected < getTotalCount());
    }

    @Override
    public List<searchengine.config.Site> getConfigSites() {
        return sitesList.getSites();
    }
}
