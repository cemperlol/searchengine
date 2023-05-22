package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchServiceResult {

    private String siteUrl;

    private String siteName;

    private String pageUrl;

    private String snippet;

    private float relevance;
}
