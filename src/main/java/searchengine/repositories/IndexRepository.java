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
public interface IndexRepository extends CommonEntityRepository<Index> {

    @Modifying
    @Transactional
    @Query("delete from Index i where i.page.id = :pageId")
    void deleteByPageId(@Param("pageId") int pageId);

    @Query("select i from Index i where i.page.id = :pageId")
    List<Index> getByPageId(@Param("pageId") int pageId);

    @Query("select i from Index i where i.lemma.id = :lemmaId and i.page.id = :pageId")
    Optional<Index> getByLemmaAndPageId(@Param("lemmaId") int lemmaId, @Param("pageId") int pageId);

    @Modifying
    @Transactional
    @Query("update Index i set i.rank = :rank where i.id = :id")
    void updateRank(@Param("rank") float rank, @Param("id") int id);
}

