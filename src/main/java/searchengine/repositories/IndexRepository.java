package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;

import java.util.List;
import java.util.Set;

@Repository
@Transactional
public interface IndexRepository extends CommonEntityRepository<Index> {

    @Modifying
    @Query(value = "INSERT INTO `index` (page_id, lemma_id, `rank`) " +
            "VALUES ((SELECT id FROM page AS p where p.site_id = :siteId AND p.path = :pagePath), " +
                    "(SELECT id FROM lemma AS l WHERE l.site_id = :siteId AND l.lemma = :lemma), " +
                    ":rank) " +
            "ON DUPLICATE KEY UPDATE `rank` = `rank` + :rank",
            nativeQuery = true)
    void save(@Param("siteId") int siteId, @Param("pagePath") String pagePath,
              @Param("lemma") String lemma, @Param("rank") float rank);

    @Query("SELECT i FROM Index i " +
            "WHERE i.page.id = :pageId " +
            "AND i.lemma.id in :lemmasId")
    Set<Index> findByPageIdAndLemmasId(@Param("pageId") int pageId, @Param("lemmasId") List<Integer> lemmasId);
}

