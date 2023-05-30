package searchengine.dao.lemma;

import searchengine.dao.DBService;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;

public interface LemmaService extends DBService {

    Lemma save(String lemmaValue, Site site);

    List<Lemma> saveAll(Collection<String> lemmaValues, Site site);

    Lemma getByLemmaAndSiteId(String lemmaValue, int siteId);

    void deletePageInfo(List<Lemma> lemmas);

    boolean filterTooFrequentLemmasOnSite(Lemma lemma, int pageCount);
}
