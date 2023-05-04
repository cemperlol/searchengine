package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class HtmlServiceImpl implements HtmlService {

    public static Document parsePage(Connection.Response response) {
        Document doc = null;

        try {
            doc = response.parse();
        } catch (IOException e) {
            ApplicationLogger.log(e);
        }

        return doc;
    }
}
