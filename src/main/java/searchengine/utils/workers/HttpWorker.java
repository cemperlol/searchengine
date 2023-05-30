package searchengine.utils.workers;

public class HttpWorker {

    public static String makeUrlWithoutWWW(String url) {
        return url.replace("://www.", "://");
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
