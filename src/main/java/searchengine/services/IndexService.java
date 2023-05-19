package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class IndexService {

    private final IndexRepository indexRepository;

    @Autowired
    public IndexService(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }
    
    public Index saveIndex(Page page, Lemma lemma, int rank) {
        Index index = new Index();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank((float) rank);

        return indexRepository.save(index);
    }

    public List<Index> saveAllIndexes(Page page, List<Lemma> lemmas, List<Integer> ranks) {
        List<Index> indexes = new ArrayList<>();

        for(int i = 0; i < lemmas.size(); i++) {
            indexes.add(createIndex(page, lemmas.get(i), ranks.get(i)));
        }
        indexRepository.saveAll(indexes);

        return indexes;
    }

    private Index createIndex(Page page, Lemma lemma, int rank) {
        Index index = new Index();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank((float) rank);

        return index;
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
