package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

@Repository
@Transactional
public interface PageRepository extends CommonEntityRepository<Page> {

    @Query("from Page where path = :path and site.id = :siteId")
    Page findByPathAndSiteId(@Param("path") String path, @Param("siteId") int siteId);
}
