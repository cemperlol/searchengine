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

    @Modifying
    @Query(value = "INSERT INTO lemma (site_id, lemma, frequency) " +
            "VALUES (:siteId, :lemma, 1) " +
            "ON DUPLICATE KEY UPDATE frequency = frequency + 1",
            nativeQuery = true)
    void save(@Param("siteId") int siteId, @Param("lemma") String lemma);

    @Query("select l from Lemma l where l.site.id = :siteId and l.lemma = :lemma")
    Lemma findBySiteIdAndLemma(@Param("siteId") int siteId, @Param("lemma") String lemma);

    @Query("select count(l) from Lemma l where l.site.id = :siteId")
    int countBySiteId(@Param("siteId") int siteId);

    @Modifying
    @Query("update Lemma l set l.frequency = l.frequency - 1 where l.id = :id")
    void decrementFrequencyById(@Param("id") int id);
}
