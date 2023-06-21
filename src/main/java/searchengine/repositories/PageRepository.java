package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

@Repository
@Transactional
public interface PageRepository extends CommonEntityRepository<Page> {

    @Query("select count(p) from Page p where p.site.id = :siteId")
    int countBySiteId(@Param("siteId") int siteId);
}
