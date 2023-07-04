package searchengine.cache;

import java.util.*;

public class PageCache {

    private static final Map<Integer, Set<String>> vault = new HashMap<>();

    public static void addPageForSite(int siteId, String page) {
        if (vault.containsKey(siteId)) {
            Set<String> currentPages = vault.get(siteId);
            currentPages.add(page);
        } else {
            Set<String> currentPages = new HashSet<>();
            currentPages.add(page);
            vault.put(siteId, currentPages);
        }
    }

    public static Set<String> getSitePages(int siteId) {
        return vault.get(siteId);
    }

    public static boolean pageIndexed(int siteId, String path) {
        if (!vault.containsKey(siteId)) return false;

        return vault.get(siteId).contains(path);
    }

    public static void clearSitePagesCache(int siteId) {
        vault.remove(siteId);
    }

    public static void clearCache() {
        vault.clear();
    }
}
