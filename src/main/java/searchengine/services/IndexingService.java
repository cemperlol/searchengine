package searchengine.services;

public interface IndexingService {

    boolean isIndexing();

    void clearDatabaseBeforeStart();

    String startIndexing();


}
