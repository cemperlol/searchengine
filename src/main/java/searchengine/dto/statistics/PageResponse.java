package searchengine.dto.statistics;

import lombok.Data;
import org.jsoup.Connection;

@Data
public class PageResponse {

    private Connection.Response response;

    private int statusCode;

    private String CauseOfError;
}
