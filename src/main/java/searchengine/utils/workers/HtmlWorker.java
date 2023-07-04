package searchengine.utils.workers;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.web.client.HttpStatusCodeException;
import searchengine.dto.page.PageResponse;
import searchengine.logging.ApplicationLogger;

import java.io.IOException;

public class HtmlWorker {

    public static PageResponse getResponse(String url) {
        PageResponse pageResponse;

        try {
            Connection.Response response = Jsoup
                    .connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
                            "Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("https://www.google.com")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();

            pageResponse = configurePageResponse(response);
        } catch (HttpStatusCodeException e) {
            pageResponse = configurePageResponse(e);
        } catch (Throwable e) {
            return null;
        }

        return pageResponse;
    }

    private static PageResponse configurePageResponse(Connection.Response response) {
        PageResponse pageResponse = new PageResponse();
        pageResponse.setStatusCode(response.statusCode());
        pageResponse.setResponseBody(response.body());
        pageResponse.setResponse(response);

        return pageResponse;
    }

    private static PageResponse configurePageResponse(HttpStatusCodeException e) {
        PageResponse pageResponse = new PageResponse();
        pageResponse.setStatusCode(e.getStatusCode().value());
        pageResponse.setResponseBody(e.getResponseBodyAsString());
        pageResponse.setCauseOfError(e.getStatusText());
        pageResponse.setResponse(null);

        return pageResponse;
    }

    public static Document parsePage(Connection.Response response) {
        Document doc = null;

        try {
            doc = response.parse();
        } catch (IOException e) {
            ApplicationLogger.logError(e);
        }

        return doc;
    }

    public static String clearFromHtml(String text) {
        Document doc = Jsoup.parse(text);
        StringBuilder sb = new StringBuilder();
        doc.select("*").forEach(e -> sb.append(e.ownText()).append(" "));

        return sb.toString().replaceAll("\\s{2,}", ". ").toLowerCase().trim();
    }

    public static String getPageTitle(String text) {
        return Jsoup.parse(text).title();
    }
}