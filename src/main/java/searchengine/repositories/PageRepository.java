package searchengine.repositories;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

import javax.persistence.LockModeType;

@Repository
@Transactional
public interface PageRepository extends CommonEntityRepository<Page> {

    @Lock(LockModeType.READ)
    @Query("select p from Page p where p.site.id = :siteId and p.path = :path")
    Page findBySiteIdAndPath(@Param("siteId") int siteId, @Param("path") String path);

    @Modifying
    @Query(value = "INSERT IGNORE INTO page (site_id, path, code, content) " +
            "VALUES (:page.site.id, :page.path, :page.code, :page.content)",
            nativeQuery = true)
    int ignoreSave(Page page);
}
