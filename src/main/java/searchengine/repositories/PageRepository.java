package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

import java.util.List;

@Repository
@Transactional
public interface PageRepository extends CommonEntityRepository<Page> {

    @Query("select count(p) from Page p where p.site.id = :siteId")
    int countBySiteId(@Param("siteId") int siteId);

    @Query("SELECT p FROM Page p " +
            "JOIN Index i ON p.id = i.page.id " +
            "JOIN Lemma l ON i.lemma.id = l.id " +
            "WHERE l.id IN :lemmaIds " +
            "GROUP BY p.id " +
            "HAVING COUNT(DISTINCT l.id) = :lemmaCount " +
            "ORDER BY SUM(i.rank) DESC")
    List<Page> findPagesByLemmaIds(@Param("lemmaIds") List<Integer> lemmaIds,
                                   @Param("lemmaCount") Integer lemmaCount);

}
