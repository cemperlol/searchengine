package searchengine.utils.responsegenerators;

import org.apache.commons.lang3.ArrayUtils;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchServiceResult;

import java.util.Arrays;
import java.util.List;

public class SearchResponseGenerator {

    public static SearchResponse failureResponse(String errorMsg) {
        return new SearchResponse(errorMsg, false);
    }

    public static SearchResponse noResults() {
        return failureResponse("No results were found");
    }

    public static SearchResponse siteNotIndexed() {
        return failureResponse("Site has not been indexed yet");
    }

    public static SearchResponse noSitesIndexed() {
        return failureResponse("No sites has been indexed yet");
    }

    public static SearchResponse emptyQuery() {
        return failureResponse("The query is empty");
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
        Arrays.sort(totalResponse.getData(), (data1, data2) ->
                Float.compare(data2.getRelevance(), data1.getRelevance()));

        return totalResponse;
    }
}
