package searchengine.dto.search;

import lombok.Data;

@Data
public class LastSearch {

    private static String site = "";

    private static String query = "";

    private static SearchResponse response = null;

    public static String getSite() {
        return site;
    }

    public static void setSite(String site) {
        LastSearch.site = site;
    }

    public static String getQuery() {
        return query;
    }

    public static void setQuery(String query) {
        LastSearch.query = query;
    }

    public static SearchResponse getResponse() {
        return response;
    }

    public static void setResponse(SearchResponse response) {
        LastSearch.response = response;
    }
}
