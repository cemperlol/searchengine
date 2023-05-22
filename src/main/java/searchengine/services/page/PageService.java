package searchengine.services.page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.page.PageResponse;
import searchengine.model.Page;
import searchengine.model.Site;

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
        page.setContent(page.getCode() >= 400 ? "Content is unknown" : pageResponse.getResponseBody());

        savePage(page);

        return page;
    }

    public int getPageCount(int siteId) {
        return pageRepository.countPageBySiteId(siteId);
    }

    public int getTotalPageCount() {
        return pageRepository.totalCountPage();
    }

    public Page findByPathAndSiteId(String path, int siteId) {
        return pageRepository.selectByPathAndSiteId(path, siteId);
    }

    public void deletePageById(int pageId) {
        pageRepository.deleteById(pageId);
    }

    public void deleteAllPages() {
        pageRepository.deleteAll();
    }
}
