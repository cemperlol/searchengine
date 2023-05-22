package searchengine.utils;

import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchServiceResult;

public class SearchResponseGenerator {

    public static SearchResponse noResults() {
        return new SearchResponse("No results were found", false);
    }

    public static SearchResponse siteNotIndexed() {
        return new SearchResponse("Site has not been indexed yet", false);
    }

    public static SearchResponse noSitesIndexed() {
        return new SearchResponse("No sites has been indexed yet", false);
    }

    public static SearchResponse resultsFound(int pageCount, SearchServiceResult[] data) {
        return new SearchResponse(true, pageCount, data);
    }
}
