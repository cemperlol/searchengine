package searchengine.repositories;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends CommonEntityRepository<Index> {

    @Modifying
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
    default void updateOrCreateIndex(Page page, Lemma lemma, float rank) {
        Index index = findByLemmaAndPageIds(lemma.getId(), page.getId());

        if (index != null) {
            increaseRankById(index.getId(), rank);
        } else {
            index = new Index();
            index.setLemma(lemma);
            index.setPage(page);
            index.setRank(rank);
            save(index);
        }
    }

    List<Index> findByPageId(@Param("pageId") int pageId);

    @Query("select i.lemma from Index i where i.page.id = :pageId")
    List<Lemma> findLemmasByPageId(@Param("pageId") int pageId);

    @Query("select i from Index i where i.lemma.id = :lemmaId and i.page.id = :pageId")
    Index findByLemmaAndPageIds(@Param("lemmaId") int lemmaId, @Param("pageId") int pageId);

    @Modifying
    @Query("delete from Index i where i.page.id = :pageId")
    void deleteByPageId(@Param("pageId") int pageId);

    @Modifying
    @Query("update Index i set i.rank = (i.rank + :rank) where i.id = :id")
    void increaseRankById(@Param("id") int id, @Param("rank") float rank);
}

