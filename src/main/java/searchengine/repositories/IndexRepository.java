package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface IndexRepository extends CommonEntityRepository<Index> {

    List<Index> findByPageId(@Param("pageId") int pageId);

    @Query("select i from Index i where i.lemma.id = :lemmaId and i.page.id = :pageId")
    Optional<Index> findByLemmaAndPageIds(@Param("lemmaId") int lemmaId, @Param("pageId") int pageId);

    @Modifying
    @Transactional
    @Query("delete from Index i where i.page.id = :pageId")
    void deleteByPageId(@Param("pageId") int pageId);

    @Modifying
    @Transactional
    @Query("update Index i set i.rank = :rank where i.id = :id")
    void updateRank(@Param("id") int id, @Param("rank") float rank);
}

