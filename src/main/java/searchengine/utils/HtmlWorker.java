package searchengine.utils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.springframework.web.client.HttpStatusCodeException;
import searchengine.dto.page.PageResponse;
import searchengine.model.Lemma;
import searchengine.services.logging.ApplicationLogger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String clearFromHtml(String text) {
        Document doc = Jsoup.parse(text);
        StringBuilder sb = new StringBuilder();
        doc.select("*").forEach(e -> {
            sb.append(e.ownText()).append(" ");
        });

        return sb.toString().replaceAll("\\s{2,}", ". ").toLowerCase().trim();
    }

    public static String getPageTitle(String text) {
        return Jsoup.parse(text).title();
    }

    public static String highlightLemmas(AtomicReference<String> textBlock, List<Lemma> lemmas) {
        lemmas.forEach(lemma -> {
            String regex = lemma.getLemma();
            String replacement = "<b>" + lemma.getLemma() + "</b>";
            textBlock.set(textBlock.get().replaceAll(regex, replacement));
        });

        return textBlock.get();
    }

    public static String highlightClosestToLemma(StringBuilder text, String rarestLemma) {
        String result = "";

        int start = text.toString().indexOf(rarestLemma.substring(0, rarestLemma.length() / 2));
        int end = text.toString().indexOf(" ", start);
        result = result.concat(text.substring(0, start)).concat("<b>");
        result = result.concat(text.substring(start, end)).concat("</b>");

        return result.concat(text.substring(end));
    }

    public static String getSnippet(List<Lemma> lemmas, String text) {
        List<AtomicReference<String>> textBlocks = Arrays.stream(text.split("([.?!]+[\\s]+)"))
                .map(AtomicReference::new)
                .toList();

        String rarestLemma = lemmas.get(0).getLemma();
        textBlocks.forEach(block -> block.set(highlightLemmas(block, lemmas)));
        StringBuilder allocatedText = new StringBuilder();
        textBlocks.forEach(block -> 
                allocatedText.append(allocatedText.isEmpty() ? "" : " ").append(block.get()));

        int start = getSnippetStart(allocatedText, rarestLemma);

        return "..."
                .concat(allocatedText.substring(start, Math.min(allocatedText.length(), start + 216)))
                .concat("...");
    }

    private static int getSnippetStart(StringBuilder allocatedText, String rarestLemma) {
        int start;

        if (!allocatedText.toString().contains("<b>")) {
            allocatedText.replace(0, allocatedText.length(), highlightClosestToLemma(allocatedText, rarestLemma));
            start = allocatedText.toString().indexOf(rarestLemma.substring(0, rarestLemma.length() / 2));
        } else {
            start = allocatedText.toString().indexOf(rarestLemma);
        }
        
        return start < 22 ? 0 : start - 22;
    }
}