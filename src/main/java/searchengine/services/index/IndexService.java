package searchengine.services.index;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

public interface IndexService extends CommonEntityService<Index> {

    Index save(Page page, Lemma lemma, int rank);

    List<Index> saveAll(Page page, List<Lemma> lemmas, List<Integer> ranks);

    List<Index> getByPageId(int pageId);

    List<Lemma> getLemmasByPageId(int pageId);

    void deleteByPageId(int pageId);
}
