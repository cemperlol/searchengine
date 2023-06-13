package searchengine.cache;

import searchengine.model.Lemma;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LemmaCache {

    private static final Map<Integer, Set<Lemma>> vault = new HashMap<>();

    public static void addLemmasForSite(int siteId, Set<Lemma> lemmas) {
        if (vault.containsKey(siteId)) {
            Set<Lemma> currentLemmas = vault.get(siteId);
            currentLemmas.addAll(lemmas);
        } else {
            vault.put(siteId, lemmas);
        }
    }

    public static Set<Lemma> getSiteLemmas(int siteId) {
        return vault.get(siteId);
    }

    public static void clearCache() {
        vault.clear();
    }
}
