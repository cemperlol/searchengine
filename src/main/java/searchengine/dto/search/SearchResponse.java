package searchengine.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SearchResponse {

    private boolean result;

    private String error = "no error";

    private int count;

    private SearchServiceResult[] data;

    public SearchResponse(String error, boolean result) {
        this.error = error;
        this.result = result;
    }

    public SearchResponse(boolean result, int count, SearchServiceResult[] data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }

    public SearchResponse(SearchResponse response) {
        this.result = response.result;
        this.error = response.error;
        this.count = response.count;
        this.data = response.data;
    }
}
