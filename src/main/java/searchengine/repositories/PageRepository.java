package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Query("from Page where path = :path and site.id = :siteId")
    Page findByPathAndSiteId(@Param("path") String path, @Param("siteId") int siteId);
}
