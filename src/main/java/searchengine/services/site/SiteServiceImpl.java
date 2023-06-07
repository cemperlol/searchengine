package searchengine.services.site;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;
import searchengine.utils.workers.HttpWorker;

import java.sql.Timestamp;
import java.util.List;

@Service
public class SiteServiceImpl extends AbstractEntityService<Site, SiteRepository>
        implements SiteService {

    private final SitesList sitesList;

    @Autowired
    public SiteServiceImpl(SiteRepository repository, SitesList sitesList) {
        super(repository);
        this.sitesList = sitesList;
    }

    @Override
    public Site save(searchengine.config.Site configSite) {
        Site site = repository.findByUrl(configSite.getUrl()).orElse(null);

        if (site == null) {
            site = new Site();
            site.setStatus(SiteStatus.INDEXING);
            site.setUrl(HttpWorker.makeUrlWithoutSlashEnd(HttpWorker.makeUrlWithoutWWW(configSite.getUrl())));
            site.setName(configSite.getName());
        }
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));

        return repository.save(site);
    }

    @Override
    public Site getByUrl(String url) {
        return repository.findByUrl(url.replace("://www.", "://")).orElse(null);
    }

    @Override
    public List<Site> getAll() {
        return repository.findAll();
    }

    @Override
    public Site updateStatus(int id, SiteStatus status) {
        repository.updateStatus(status, id);

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
        repository.updateStatusTime(id);

        return repository.findById(id).orElse(null);
    }

    @Override
    public Site updateLastError(int id, String lastError) {
        repository.updateLastError(lastError, id);
        updateStatus(id, SiteStatus.FAILED);

        return updateStatus(id, SiteStatus.FAILED);
    }

    @Override
    public List<searchengine.config.Site> getConfigSites() {
        return sitesList.getSites();
    }
}
