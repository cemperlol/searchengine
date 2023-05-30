package searchengine.dao.page;

import searchengine.dao.DBService;
import searchengine.dto.page.PageResponse;
import searchengine.model.Page;
import searchengine.model.Site;

public interface PageService extends DBService {

    Page save(PageResponse pageResponse, Site site);

    Page getByPathAndSiteId(String path, int siteId);


}
