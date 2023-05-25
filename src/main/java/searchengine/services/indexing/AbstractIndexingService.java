package searchengine.services.indexing;

import searchengine.dto.indexing.IndexingToggleResponse;

import java.util.concurrent.RecursiveTask;

public abstract class AbstractIndexingService extends RecursiveTask<IndexingToggleResponse>
        implements IndexingService{

    @Override
    public abstract IndexingToggleResponse startIndexing();

    @Override
    public abstract IndexingToggleResponse stopIndexing();

    @Override
    public abstract IndexingToggleResponse indexPage(String url);

    @Override
    public abstract IndexingToggleResponse compute();
}
