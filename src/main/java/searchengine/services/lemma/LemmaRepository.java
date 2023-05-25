package searchengine.services.lemma;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

import java.util.Optional;

public interface LemmaRepository extends CrudRepository<Lemma, Integer> {

    @Query("select l from Lemma l where l.lemma = :lemmaValue and l.site.id = :siteId")
    Optional<Lemma> getByLemmaAndSiteId(@Param("lemmaValue") String lemmaValue,
                                        @Param("siteId") int siteId);

    @Modifying
    @Transactional
    @Query("update Lemma l set l.frequency = :frequency where l.id = :id")
    void updateLemmaFrequencyById(@Param("id") int id, @Param("frequency") int frequency);

    @Query("select count(lemma) from Lemma where site.id = :siteId")
    Integer countLemmasBySiteId(@Param("siteId") int site);
}
