package searchengine.services.page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
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
        if (findByPathAndSiteId(pageResponse.getPath(), site.getId()) != null) return null;
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
        return pageRepository.totalPageCount();
    }

    public int getMinId() {
        return pageRepository.minId();
    }

    public Page findByPathAndSiteId(String path, int siteId) {
        return pageRepository.selectByPathAndSiteId(path, siteId);
    }

    public void deletePageById(int pageId) {
        pageRepository.deleteById(pageId);
    }

    public void deleteAll() {
        int batchSize = 500;
        int totalRowsAffected = 0;

        do {
            pageRepository.deleteAllInBatches(batchSize);
            totalRowsAffected += batchSize;
        } while (totalRowsAffected < getTotalPageCount());
    }
}
