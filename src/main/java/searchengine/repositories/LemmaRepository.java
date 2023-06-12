package searchengine.repositories;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

import javax.persistence.LockModeType;

@Repository
@Transactional
public interface LemmaRepository extends CommonEntityRepository<Lemma> {

    @Modifying
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
    default Lemma updateOrCreateLemma(Site site, String lemmaValue) {
        Lemma lemma = findByLemmaAndSiteId(site.getId(), lemmaValue);

        if (lemma != null) {
            incrementFrequencyById(lemma.getId());
        } else {
            lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(lemmaValue);
            lemma.setFrequency(1);
            lemma = save(lemma);
        }

        return lemma;
    }

    @Query("select l from Lemma l where l.lemma = :lemmaValue and l.site.id = :siteId")
    Lemma findByLemmaAndSiteId(@Param("siteId") int siteId,
                               @Param("lemmaValue") String lemmaValue);

    @Modifying
    @Query("update Lemma l set l.frequency = (l.frequency + 1) where l.id = :id")
    void incrementFrequencyById(@Param("id") int id);

    @Modifying
    @Query("update Lemma l set l.frequency = (l.frequency - 1) where l.id = :id")
    void decrementFrequencyById(@Param("id") int id);
}
