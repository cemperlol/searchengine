package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

import java.util.List;


@Repository
@Transactional
public interface LemmaRepository extends CommonEntityRepository<Lemma> {

    @Query("select l from Lemma l where l.site.id = :siteId")
    List<Lemma> findBySiteId(@Param("siteId") int siteId);

    @Modifying
    @Query("update Lemma l set l.frequency = (l.frequency + 1) where l.id = :id")
    void incrementFrequencyById(@Param("id") int id);

    @Modifying
    @Query("update Lemma l set l.frequency = (l.frequency - 1) where l.id = :id")
    void decrementFrequencyById(@Param("id") int id);
}
