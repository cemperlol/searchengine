package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class LemmaService {

    private final LemmaRepository lemmaRepository;

    @Autowired
    public LemmaService(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    public Lemma getLemmaById(int id) {
        return lemmaRepository.findById(id).orElse(null);
    }

    public Lemma getByLemmaAndSiteId(String lemmaValue, int siteId) {
        return  lemmaRepository.getByLemmaAndSiteId(lemmaValue, siteId).orElse(null);
    }

    @Transactional
    public List<Lemma> saveAllLemmas(Collection<String> lemmaValues, Site site) {
        List<Lemma> lemmas = new ArrayList<>();

        lemmaValues.forEach(lemmaValue -> {
            lemmas.add(createLemma(lemmaValue, site));
        });

        lemmaRepository.saveAll(lemmas);
        return lemmas;
    }

    public Lemma saveLemma(String lemmaValue, Site site) {
        return lemmaRepository.save(createLemma(lemmaValue, site));
    }

    private Lemma createLemma(String lemmaValue, Site site) {
        Lemma lemma = getByLemmaAndSiteId(lemmaValue, site.getId());
        if (lemma == null) {
            lemma = new Lemma();
            lemma.setLemma(lemmaValue);
            lemma.setSite(site);
            lemma.setFrequency(1);
        } else {
            lemma.setFrequency(lemma.getFrequency() + 1);
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
        lemmaRepository.deleteAll();
    }
}
