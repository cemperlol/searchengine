package searchengine.services.lemma;

import searchengine.services.CommonEntityService;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;

public interface LemmaService extends CommonEntityService<Lemma> {

    Lemma save(Site site, String lemmaValue);

    List<Lemma> saveAll(Collection<String> lemmaValues, Site site);

    Lemma getByLemmaAndSiteId(String lemmaValue, int siteId);

    void deletePageInfo(List<Lemma> lemmas);

    boolean filterTooFrequentLemmasOnSite(Lemma lemma, int pageCount);
}
