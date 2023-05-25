package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchCache {

    private static String query = "";

    private static SearchResponse response = null;

    public static String getQuery() {
        return query;
    }

    public static void setQuery(String query) {
        SearchCache.query = query;
    }

    public static SearchResponse getResponse() {
        return response;
    }

    public static void setResponse(SearchResponse response) {
        SearchCache.response = response;
    }
}
