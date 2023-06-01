package searchengine.services.site;

import searchengine.services.CommonEntityService;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.util.List;

public interface SiteService extends CommonEntityService<Site> {

    Site save(searchengine.config.Site configSite);

    Site getByUrl(String url);

    List<Site> getAll();

    Site updateStatus(int id, SiteStatus status);

    Site updateIncorrectShutdown(Site site);

    Site updateStatusTime(int id);

    Site updateLastError(int id, String lastError);

    List<searchengine.config.Site> getConfigSites();
}
