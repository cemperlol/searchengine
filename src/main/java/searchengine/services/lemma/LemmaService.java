package searchengine.services.lemma;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class LemmaService {

    private final LemmaRepository lemmaRepository;

    @Autowired
    public LemmaService(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    public int getLemmaCountBySiteId(int siteId) {
        return lemmaRepository.countLemmasBySiteId(siteId);
    }

    public int getTotalLemmaCount() {
        return lemmaRepository.totalCountLemmas();
    }

    public int getMinId() {
        return lemmaRepository.minId();
    }

    public Lemma getLemmaById(int id) {
        return lemmaRepository.findById(id).orElse(null);
    }

    public List<Lemma> getAllByLemmaAndSiteId(List<String> lemmaValues, int siteId) {
        List<Lemma> lemmas = new ArrayList<>();
        lemmaValues.forEach(v -> lemmas.add(getByLemmaAndSiteId(v, siteId)));

        return lemmas.stream().filter(Objects::nonNull).toList();
    }

    public Lemma getByLemmaAndSiteId(String lemmaValue, int siteId) {
        return lemmaRepository.getByLemmaAndSiteId(lemmaValue, siteId).orElse(null);
    }

    public List<Lemma> saveAllLemmas(Collection<String> lemmaValues, Site site) {
        return lemmaValues.stream()
                .map(lemmaValue -> saveLemma(lemmaValue, site))
                .toList();
    }

    public Lemma saveLemma(String lemmaValue, Site site) {
        Lemma lemma = getByLemmaAndSiteId(lemmaValue, site.getId());
        if (lemma == null) {
            lemma = new Lemma();
            lemma.setLemma(lemmaValue);
            lemma.setSite(site);
            lemma.setFrequency(1);

            lemmaRepository.save(lemma);
        } else {
            lemmaRepository.updateLemmaFrequencyById(lemma.getId(), lemma.getFrequency() + 1);
        }

        return lemma;
    }

    public void deletePageInfo(List<Lemma> lemmas) {
        lemmas.forEach(lemma -> {
            if (lemma.getFrequency() == 1.0) lemmaRepository.deleteById(lemma.getId());
            else lemmaRepository.updateLemmaFrequencyById(lemma.getId(), lemma.getFrequency() - 1);
        });
    }

    public void deleteAll() {
        int batchSize = 500;
        int totalRowsAffected = 0;
        do {
            lemmaRepository.deleteAllInBatches(batchSize);
            totalRowsAffected += batchSize;
        } while (totalRowsAffected < getTotalLemmaCount());
    }

    public boolean filterTooFrequentLemmasOnSite(Lemma lemma, int pageCount) {
        if (pageCount < 10) return true;
        return (float) lemma.getFrequency() / pageCount < 0.25;
    }
}
