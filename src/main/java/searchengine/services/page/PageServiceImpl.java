package searchengine.services.page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.page.PageResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

@Service
public class PageServiceImpl extends AbstractEntityService<Page, PageRepository>
        implements PageService {
    
    @Autowired
    public PageServiceImpl(PageRepository repository) {
        super(repository);
    }

    @Override
    public Page save(PageResponse pageResponse, Site site) {
        Page page = getByPathAndSiteId(pageResponse.getPath(), site.getId());

        if (page == null) {
            page = new Page();
            page.setSite(site);
            page.setPath(pageResponse.getPath());
            page.setCode(pageResponse.getStatusCode());
            page.setContent(page.getCode() >= 400 ? "" : pageResponse.getResponseBody());
        }

        return repository.save(page);
    }

    @Override
    public Page getByPathAndSiteId(String path, int siteId) {
        return repository.findByPathAndSiteId(path, siteId);
    }
}
