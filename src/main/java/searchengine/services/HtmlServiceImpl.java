package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;

public class HtmlServiceImpl implements HtmlService {

    @Override
    public Connection.Response getResponse(String url) {
        Connection.Response response = null;

        try {
            response = Jsoup
                    .connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
                            "Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .execute();

        } catch (HttpStatusCodeException e) {
            //TODO: add handler for such exceptions
//            ApplicationLogger.log(e);
        } catch (IOException e) {
            ApplicationLogger.log(e);
        }

        return response;
    }

    @Override
    public Document parsePage(Connection.Response response) {
        Document doc = null;

        try {
            doc = response.parse();
        } catch (IOException e) {
            ApplicationLogger.log(e);
        }

        return doc;
    }
}
