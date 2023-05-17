package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;

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
    public Lemma getLemmaByLemmaValueAndSiteId(String lemmaValue, int siteId) {
        return lemmaRepository.getLemmaByLemmaValueAndSiteId(lemmaValue, siteId).orElse(null);
    }

    public Lemma saveLemma(String lemmaValue, Site site) {
        Lemma lemma = getLemmaByLemmaValueAndSiteId(lemmaValue, site.getId());
        if (lemma == null) {
            lemma = new Lemma();
            lemma.setLemma(lemmaValue);
            lemma.setSite(site);
            lemma.setFrequency(1);
            return lemmaRepository.save(lemma);
        }

        return lemmaRepository.updateLemmaFrequencyById(lemma.getId(), lemma.getFrequency() + 1);
    }
}
