package searchengine.services;

import searchengine.config.Site;

import java.util.List;

public interface IndexingService {

    void clearTablesBeforeStart();

    void startIndexing(List<Site> sites);
}
