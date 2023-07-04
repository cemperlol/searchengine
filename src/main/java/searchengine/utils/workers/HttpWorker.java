package searchengine.utils.workers;

public class HttpWorker {

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
}
