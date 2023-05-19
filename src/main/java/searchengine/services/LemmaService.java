package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class LemmaService {

    private static LemmaRepository lemmaRepository;

    @Autowired
    public static void setLemmaRepository(LemmaRepository lemmaRepository) {
        LemmaService.lemmaRepository = lemmaRepository;
    }

    public static Lemma getLemmaById(int id) {
        return lemmaRepository.findById(id).orElse(null);
    }
    public static Lemma getByLemmaAndSiteId(String lemmaValue, int siteId) {
        return lemmaRepository.getByLemmaAndSiteId(lemmaValue, siteId).orElse(null);
    }

    public static List<Lemma> saveAllLemmas(Collection<String> lemmaValues, Site site) {
        List<Lemma> lemmas = new ArrayList<>();

        lemmaValues.forEach(lemmaValue -> {
            Lemma lemma = getByLemmaAndSiteId(lemmaValue, site.getId());
            if (lemma == null) {
                lemma = new Lemma();
                lemma.setLemma(lemmaValue);
                lemma.setSite(site);
                lemma.setFrequency(1);
            } else {
                lemma.setFrequency(lemma.getFrequency() + 1);
            }

            lemmas.add(lemma);
        });

        lemmaRepository.saveAll(lemmas);
        return lemmas;
    }

    public static Lemma saveLemma(String lemmaValue, Site site) {
        Lemma lemma = getByLemmaAndSiteId(lemmaValue, site.getId());
        if (lemma == null) {
            lemma = new Lemma();
            lemma.setLemma(lemmaValue);
            lemma.setSite(site);
            lemma.setFrequency(1);

            return lemmaRepository.save(lemma);
        }

        lemmaRepository.updateLemmaFrequencyById(lemma.getId(), lemma.getFrequency() + 1);
        return lemmaRepository.findById(lemma.getId()).orElse(null);
    }

    public static void deletePageInfo(List<Lemma> lemmas) {
        lemmas.forEach(lemma -> {
            if (lemma.getFrequency() == 1.0) lemmaRepository.deleteById(lemma.getId());
            else lemmaRepository.updateLemmaFrequencyById(lemma.getId(), lemma.getFrequency() - 1);
        });
    }

    public static void deleteAll() {
        lemmaRepository.deleteAll();
    }
}
