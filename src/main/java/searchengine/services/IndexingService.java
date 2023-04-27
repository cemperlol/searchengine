package searchengine.services;

public interface IndexingService {

    void clearDatabaseBeforeStart();

    void startIndexing();
}
