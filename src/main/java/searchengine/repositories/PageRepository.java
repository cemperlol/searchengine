package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface PageRepository extends CommonEntityRepository<Page> {

    @Query("select p from Page p where p.path = :path and p.site.id = :siteId")
    Page findByPathAndSiteId(@Param("path") String path, @Param("siteId") int siteId);
}
