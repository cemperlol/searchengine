package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;


@Repository
@Transactional
public interface LemmaRepository extends CommonEntityRepository<Lemma> {

    @Modifying
    @Query("update Lemma l set l.frequency = (l.frequency + 1) where l.id = :id")
    void incrementFrequencyById(@Param("id") int id);

    @Modifying
    @Query("update Lemma l set l.frequency = (l.frequency - 1) where l.id = :id")
    void decrementFrequencyById(@Param("id") int id);
}
