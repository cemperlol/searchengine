package searchengine.services.indexing;

import searchengine.dto.indexing.IndexingToggleResponse;

public interface IndexingService {

    IndexingToggleResponse startIndexing();

    IndexingToggleResponse stopIndexing();

    IndexingToggleResponse indexPage(String url);
}
