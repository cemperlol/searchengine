package searchengine.services.page;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

public interface PageRepository extends CrudRepository<Page, Integer> {

    @Query("from Page where path = :path and site.id = :siteId")
    Page selectByPathAndSiteId(@Param("path") String path, @Param("siteId") int siteId);

    @Query("select count(path) from Page where site.id = :siteId")
    Integer countPageBySiteId(@Param("siteId") int siteId);

    @Query("select count(path) from Page")
    Integer totalCountPage();
}
