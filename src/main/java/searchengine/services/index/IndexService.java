package searchengine.services.index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
import java.util.stream.IntStream;

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
        List<Index> indexes = IntStream.range(0, lemmas.size())
                .mapToObj(i -> createIndex(page, lemmas.get(i), ranks.get(i)))
                .toList();

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

    public List<Index> getByLemmaId(int lemmaId) {
        return indexRepository.getByLemmaId(lemmaId);
    }

    public List<Index> getAllByLemmasId(List<Lemma> lemmas) {
        return lemmas.stream()
                .map(Lemma::getId)
                .flatMap(lemmaId -> getByLemmaId(lemmaId).stream())
                .toList();
    }

    public List<Index> getByPageId(int pageId) {
        return indexRepository.getByPageId(pageId);
    }

    public List<Index> getAllByPagesId(List<Page> pages) {
        return pages.stream()
                .map(Page::getId)
                .flatMap(pageId -> getByPageId(pageId).stream())
                .toList();
    }

    public List<Lemma> getLemmasByPageId(int pageId) {
        return getByPageId(pageId).stream()
                .map(Index::getLemma)
                .toList();
    }

    public void deleteIndexByPageId(int pageId) {
        indexRepository.deleteIndexByPageId(pageId);
    }

    public void deleteAll() {
        indexRepository.deleteAll();
    }

    public boolean containsLemma(int lemmaId, Index targetIndex) {
        return getByLemmaId(lemmaId).stream()
                .anyMatch(index -> index.getLemma().equals(targetIndex.getLemma()));
    }
}
