package searchengine.dao.index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class IndexServiceImpl implements IndexService {

    private final IndexRepository indexRepository;

    @Autowired
    public IndexServiceImpl(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    @Override
    public Index save(Page page, Lemma lemma, int rank) {
        Index index = indexRepository.getByLemmaAndPageId(page.getId(), lemma.getId()).orElse(null);

        if (index == null) {
            index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank((float) rank);

            indexRepository.save(index);
        } else {
            indexRepository.updateRank(index.getRank() + rank, index.getId());
        }

        return index;
    }

    @Override
    public List<Index> saveAll(Page page, List<Lemma> lemmas, List<Integer> ranks) {
        return IntStream.range(0, lemmas.size())
                .mapToObj(i -> save(page, lemmas.get(i), ranks.get(i)))
                .toList();
    }

    @Override
    public int getTotalCount() {
        return indexRepository.totalCount();
    }

    @Override
    public List<Index> getByPageId(int pageId) {
        return indexRepository.getByPageId(pageId);
    }

    @Override
    public List<Lemma> getLemmasByPageId(int pageId) {
        return getByPageId(pageId).stream()
                .map(Index::getLemma)
                .toList();
    }

    @Override
    public void deleteById(int id) {
        indexRepository.deleteById(id);
    }

    @Override
    public void deleteByPageId(int pageId) {
        indexRepository.deleteByPageId(pageId);
    }

    @Override
    public void deleteAll() {
        int batchSize = 500;
        int totalRowsAffected = 0;
        do {
            indexRepository.deleteAllInBatches(batchSize);
            totalRowsAffected += batchSize;
        } while (totalRowsAffected < getTotalCount());
    }
}
