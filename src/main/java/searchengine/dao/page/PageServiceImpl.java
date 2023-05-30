package searchengine.dao.page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.page.PageResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

@Service
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;

    @Autowired
    public PageServiceImpl(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Override
    public Page save(PageResponse pageResponse, Site site) {
        Page page = getByPathAndSiteId(pageResponse.getPath(), site.getId());

        if (page == null) {
            page = new Page();
            page.setSite(site);
            page.setPath(pageResponse.getPath());
            page.setCode(pageResponse.getStatusCode());
            page.setContent(page.getCode() >= 400 ? "Content is unknown" : pageResponse.getResponseBody());
        }

        return pageRepository.save(page);
    }

    @Override
    public int getTotalCount() {
        return pageRepository.totalCount();
    }

    @Override
    public Page getByPathAndSiteId(String path, int siteId) {
        return pageRepository.findByPathAndSiteId(path, siteId);
    }

    @Override
    public void deleteById(int pageId) {
        pageRepository.deleteById(pageId);
    }

    @Override
    public void deleteAll() {
        int batchSize = 500;
        int totalRowsAffected = 0;

        do {
            pageRepository.deleteAllInBatches(batchSize);
            totalRowsAffected += batchSize;
        } while (totalRowsAffected < getTotalCount());
    }
}
