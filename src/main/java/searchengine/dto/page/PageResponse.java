package searchengine.dto.page;

import lombok.Data;
import org.jsoup.Connection;

@Data
public class PageResponse {

    private String path;

    private Connection.Response response;

    private int statusCode;

    private String responseBody;

    private String causeOfError;
}
