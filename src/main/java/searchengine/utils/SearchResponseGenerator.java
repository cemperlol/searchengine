package searchengine.utils;

import org.apache.commons.lang3.ArrayUtils;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchServiceResult;

import java.util.List;

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

    public static SearchResponse globalSearchResult(List<SearchResponse> sitesResponses) {
        SearchResponse totalResponse = new SearchResponse();
        totalResponse.setResult(true);
        sitesResponses.forEach(response -> {
            totalResponse.setCount(totalResponse.getCount() + response.getCount());
            totalResponse.setData(ArrayUtils.addAll(totalResponse.getData(), response.getData()));
        });

        return totalResponse;
    }
}
