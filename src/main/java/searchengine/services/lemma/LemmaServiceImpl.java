package searchengine.services.lemma;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import java.util.Collection;
import java.util.List;

@Service
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;

    @Autowired
    public LemmaServiceImpl(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public synchronized  Lemma save(Site site, String lemmaValue) {
        return lemmaRepository.save(createLemma(site, lemmaValue));
    }

    @Override
    public List<Lemma> saveAll(Collection<String> lemmaValues, Site site) {
        return lemmaValues.stream()
                .map(lemmaValue -> save(site, lemmaValue))
                .toList();
    }

    @Override
    public int getTotalCount() {
        return (int) lemmaRepository.count();
    }

    @Override
    public Lemma getByLemmaAndSiteId(String lemmaValue, int siteId) {
        return lemmaRepository.getByLemmaAndSiteId(lemmaValue, siteId).orElse(null);
    }

    @Override
    public void deleteById(int id) {
        lemmaRepository.deleteById(id);
    }

    @Override
    public void deletePageInfo(List<Lemma> lemmas) {
        lemmas.forEach(lemma -> {
            if (lemma.getFrequency() == 1.0) lemmaRepository.deleteById(lemma.getId());
            else lemmaRepository.updateFrequencyById(lemma.getId(), lemma.getFrequency() - 1);
        });
    }

    @Override
    public void deleteAll() {
        lemmaRepository.deleteAllInBatch();
    }

    @Override
    public boolean filterTooFrequentLemmasOnSite(Lemma lemma, int pageCount) {
        if (pageCount < 10) return true;
        return (float) lemma.getFrequency() / pageCount < 0.25;
    }

    private Lemma createLemma(Site site, String lemmaValue) {
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
}

