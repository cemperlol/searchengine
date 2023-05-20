package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import searchengine.dto.page.PageResponse;

import java.io.IOException;

@Service
public abstract class HtmlService {

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
            ApplicationLogger.log(e);
        }

        return doc;
    }

    public static String makeUrlWithoutSlashEnd(String url) {
        int urlLastSymbolIndex = url.length() - 1;
        return url.charAt(urlLastSymbolIndex) == '/' ? url.substring(0, urlLastSymbolIndex) : url;
    }

    public static String makeUrlWithSlashEnd(String url) {
        return makeUrlWithoutSlashEnd(url).concat("/");
    }

    public static String getUrlWithoutDomainName(String siteUrl, String pageUrl) {
        String domainName = siteUrl.substring(siteUrl.indexOf(".") + 1);
        return pageUrl.substring(pageUrl.indexOf(domainName) + domainName.length());
    }

    public static String getBaseUrl(String url) {
        url = makeUrlWithSlashEnd(url);

        return url.substring(0, url.indexOf("/", url.indexOf("://") + 3));
    }
}
