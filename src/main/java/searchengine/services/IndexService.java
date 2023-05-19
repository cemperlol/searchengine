package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class IndexService {

    private final IndexRepository indexRepository;

    @Autowired
    public IndexService(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }
    
    public Index saveIndex(Page page, Lemma lemma, int frequency) {
        Index index = new Index();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank((float) frequency);

        return indexRepository.save(index);
    }

    public List<Lemma> getLemmasByPageId(int pageId) {
        List<Lemma> lemmas = new ArrayList<>();
        indexRepository.getIndexesByPageId(pageId).forEach(i -> lemmas.add(i.getLemma()));

        return lemmas;
    }

    public void deleteIndexByPageId(int pageId) {
        indexRepository.deleteIndexByPageId(pageId);
    }

    public void deleteAll() {
        indexRepository.deleteAll();
    }
}
