package searchengine.utils.workers;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class UrlWorker {

    private static final Set<String> NOT_HTML_FORMATS;

    static {
        NOT_HTML_FORMATS = new HashSet<>();
        NOT_HTML_FORMATS.add(".jpg");
        NOT_HTML_FORMATS.add(".png");
        NOT_HTML_FORMATS.add(".gif");
        NOT_HTML_FORMATS.add(".svg");
        NOT_HTML_FORMATS.add(".pdf");
        NOT_HTML_FORMATS.add(".txt");
        NOT_HTML_FORMATS.add(".doc");
        NOT_HTML_FORMATS.add(".docx");
        NOT_HTML_FORMATS.add(".mp3");
        NOT_HTML_FORMATS.add(".mp4");
    }

    public static String removeWwwFromUrl(String url) {
        return url.replace("://www.", "://");
    }

    public static String removeSlashFromUrlEnd(String url) {
        int urlLastSymbolIndex = url.length() - 1;
        return url.charAt(urlLastSymbolIndex) == '/' ? url.substring(0, urlLastSymbolIndex) : url;
    }

    public static String appendSlashToUrlEnd(String url) {
        return removeSlashFromUrlEnd(url).concat("/");
    }

    public static String removeDomainFromUrl(String siteUrl, String pageUrl) {
        String domainName = siteUrl.substring(siteUrl.indexOf(".") + 1);
        return pageUrl.substring(pageUrl.indexOf(domainName) + domainName.length());
    }

    public static String getBaseUrl(String url) {
        url = appendSlashToUrlEnd(url);

        return url.substring(0, url.indexOf("/", url.indexOf("://") + 3));
    }

    public static boolean isUrlValid(String siteUrl, String pageUrl) {
        final String loweCasePageUrl = pageUrl.toLowerCase();
        Pattern sitePattern = Pattern.compile(siteUrl);

        return sitePattern.matcher(UrlWorker.removeWwwFromUrl(pageUrl)).find()
                && !pageUrl.contains("#")
                && !pageUrl.contains("?")
                && NOT_HTML_FORMATS.stream().noneMatch(loweCasePageUrl::contains);
    }
}
