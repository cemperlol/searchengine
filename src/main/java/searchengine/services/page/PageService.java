package searchengine.services.page;

import searchengine.services.CommonEntityService;
import searchengine.dto.page.PageResponse;
import searchengine.model.Page;
import searchengine.model.Site;

public interface PageService extends CommonEntityService<Page> {

    Page save(PageResponse pageResponse, Site site);

    Page getByPathAndSiteId(String path, int siteId);


}
