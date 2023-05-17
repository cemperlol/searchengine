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
public abstract class IndexService {

    private static IndexRepository indexRepository;

    @Autowired
    public static void setIndexRepository(IndexRepository indexRepository) {
        IndexService.indexRepository = indexRepository;
    }
    
    public static Index saveIndex(Page page, Lemma lemma, int frequency) {
        Index index = new Index();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank((float) frequency);

        return indexRepository.save(index);
    }

    public static List<Lemma> getLemmasByPageId(int pageId) {
        List<Lemma> lemmas = new ArrayList<>();
        indexRepository.getIndexesByPageId(pageId).forEach(i -> lemmas.add(i.getLemma()));

        return lemmas;
    }

    public static void deleteIndexByPageId(int pageId) {
        indexRepository.deleteIndexByPageId(pageId);
    }

    public static void deleteAll() {
        indexRepository.deleteAll();
    }
}
