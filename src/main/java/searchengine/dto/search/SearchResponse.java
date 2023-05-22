package searchengine.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class SearchResponse {

    public SearchResponse(String error, boolean result) {
        this.error = error;
        this.result = result;
    }

    public SearchResponse(boolean result, int count, SearchServiceResult[] data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }

    private boolean result;

    private String error = "no error";

    private int count;

    private SearchServiceResult[] data;
}
