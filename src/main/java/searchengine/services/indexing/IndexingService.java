package searchengine.services.indexing;

import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Map;

public interface IndexingService {

    IndexingStatusResponse startIndexing();

    IndexingStatusResponse stopIndexing();

    IndexingStatusResponse indexPage(String url);

    void indexParsedData(Site site, Page page, Map<String, Integer> lemmasAndFrequencies);
}
