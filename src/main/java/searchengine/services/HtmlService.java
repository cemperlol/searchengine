package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;

public interface HtmlService {

    Connection.Response getResponse(String url);

    Document parsePage(Connection.Response response);
}
