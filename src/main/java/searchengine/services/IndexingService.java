package searchengine.services;

import searchengine.dto.indexing.IndexingToggleResponse;

public interface IndexingService {

    void clearTablesBeforeStart();

    IndexingToggleResponse startIndexing();

    IndexingToggleResponse stopIndexing();
}
