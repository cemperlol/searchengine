package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.PageResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.util.List;

@Service
public class PageService {

    private final PageRepository pageRepository;

    @Autowired
    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    public Page savePage(Page page) {
        return pageRepository.save(page);
    }

    public Page savePage(PageResponse pageResponse, Site site) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(pageResponse.getPath());
        page.setCode(pageResponse.getStatusCode());
        page.setContent(pageResponse.getResponseBody());

        savePage(page);

        return page;
    }

    public Page findByPathAndSiteId(String path, int siteId) {
        return pageRepository.selectByPathAndSiteId(path, siteId);
    }

    public void deleteAllPages() {
        pageRepository.deleteAll();
    }
}
