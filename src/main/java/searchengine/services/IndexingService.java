package searchengine.services;

import searchengine.config.Site;
import searchengine.dto.statistics.IndexingResult;

import java.util.List;

public interface IndexingService {

    void clearTablesBeforeStart();

    List<IndexingResult> startIndexing();

    String stopIndexing();
}
