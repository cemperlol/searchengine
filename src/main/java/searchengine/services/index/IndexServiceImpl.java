package searchengine.services.index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;
import searchengine.services.AbstractEntityService;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
public class IndexServiceImpl extends AbstractEntityService<Index, IndexRepository>
        implements IndexService {

    @Autowired
    public IndexServiceImpl(IndexRepository repository) {
        super(repository);
    }

    @Override
    public synchronized Index save(Page page, Lemma lemma, int rank) {
        Index index = createIndex(page, lemma, rank);

        try {
            return repository.save(index);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Index> saveAll(Page page, List<Lemma> lemmas, List<Integer> ranks) {
        return IntStream.range(0, lemmas.size())
                .mapToObj(i -> save(page, lemmas.get(i), ranks.get(i)))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Index> getByPageId(int pageId) {
        return repository.getByPageId(pageId);
    }

    @Override
    public List<Lemma> getLemmasByPageId(int pageId) {
        return getByPageId(pageId).stream()
                .map(Index::getLemma)
                .toList();
    }

    @Override
    public void deleteByPageId(int pageId) {
        repository.deleteByPageId(pageId);
    }

    private Index createIndex(Page page, Lemma lemma, int rank) {
        Index index = repository.getByLemmaAndPageId(page.getId(), lemma.getId()).orElse(null);

        if (index == null) {
            index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank((float) rank);
        } else {
            updateIndex(index, rank);
        }

        return index;
    }

    private void updateIndex(Index index, int rank) {
        index.setRank(index.getRank() + rank);
    }
}
