package searchengine.utils.responseGenerators;

import searchengine.dto.indexing.IndexingToggleResponse;

public class IndexingResponseGenerator {

    public static IndexingToggleResponse successResponse() {
        return new IndexingToggleResponse(true);
    }

    public static IndexingToggleResponse createFailureResponse(String errorMsg) {
        return new IndexingToggleResponse(false, errorMsg);
    }

    public static IndexingToggleResponse siteNotAdded() {
        return createFailureResponse("This website was not added to the site list");
    }

    public static IndexingToggleResponse contentUnavailable(String fullUrl) {
        return createFailureResponse("Failed to index page, " + fullUrl + " content unavailable");
    }

    public static IndexingToggleResponse noIndexingRunning() {
        return createFailureResponse("No indexing is running");
    }

    public static IndexingToggleResponse indexingAlreadyStarted() {
        return createFailureResponse("Indexing already started");
    }

    public static IndexingToggleResponse userStoppedIndexing() {
        return createFailureResponse("User stopped indexing");
    }

    public static IndexingToggleResponse failedToCompleteIndexingTasks() {
        return createFailureResponse("Failed to complete indexing tasks");
    }

    public static IndexingToggleResponse failedToGetIndexingTasksResult() {
        return createFailureResponse("Failed to get indexing tasks result");
    }
}
