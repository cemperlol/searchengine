package searchengine.services.index;

import searchengine.services.DBService;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

public interface IndexService extends DBService {

    Index save(Page page, Lemma lemma, int rank);

    List<Index> saveAll(Page page, List<Lemma> lemmas, List<Integer> ranks);

    int getTotalCount();

    List<Index> getByPageId(int pageId);

    List<Lemma> getLemmasByPageId(int pageId);

    void deleteByPageId(int pageId);

    void deleteAll();
}
