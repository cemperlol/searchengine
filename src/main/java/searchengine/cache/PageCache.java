package searchengine.cache;

import searchengine.model.Lemma;

import java.util.*;

public class PageCache {

    private static final Map<Integer, Set<String>> VAULT = new HashMap<>();

    public static void addPageForSite(int siteId, String page) {
        if (VAULT.containsKey(siteId)) {
            Set<String> currentPages = VAULT.get(siteId);
            currentPages.add(page);
        } else {
            Set<String> currentPages = new HashSet<>();
            currentPages.add(page);
            VAULT.put(siteId, currentPages);
        }
    }

    public static Set<String> getSitePages(int siteId) {
        return VAULT.get(siteId);
    }

    public static boolean pageExists(int siteId, String path) {
        if (!VAULT.containsKey(siteId)) return false;

        return VAULT.get(siteId).contains(path);
    }

    public static void clearCache() {
        VAULT.clear();
    }
}
