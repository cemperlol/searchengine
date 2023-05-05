package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import searchengine.dto.statistics.PageResponse;

import java.io.IOException;

@Service
public interface HtmlService {

    static PageResponse getResponse(String url) {
        PageResponse pageResponse = new PageResponse();
        Connection.Response response = null;

        try {
            response = Jsoup
                    .connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
                            "Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("https://www.google.com")
                    .execute();

            pageResponse.setStatusCode(response.statusCode());
        } catch (HttpStatusCodeException e) {
            pageResponse.setStatusCode(e.getStatusCode().value());
            pageResponse.setCauseOfError(e.getStatusText());

            ApplicationLogger.log(e);
        } catch (IOException e) {
            pageResponse.setStatusCode(200);
            pageResponse.setCauseOfError("Unhandled content type");

            ApplicationLogger.log(e);
        }

        pageResponse.setResponse(response);

        return pageResponse;
    }

    static Document parsePage(Connection.Response response) {
        Document doc = null;

        try {
            doc = response.parse();
        } catch (IOException e) {
            ApplicationLogger.log(e);
        }

        return doc;
    }
}
