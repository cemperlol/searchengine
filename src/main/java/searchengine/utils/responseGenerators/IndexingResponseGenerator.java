package searchengine.utils.responseGenerators;

import searchengine.dto.indexing.IndexingToggleResponse;

public class IndexingResponseGenerator {

    public static IndexingToggleResponse successResponse() {
        return new IndexingToggleResponse(true);
    }

    public static IndexingToggleResponse createFailureResponse(String errorMsg) {
        return new IndexingToggleResponse(false, errorMsg);
    }

    public static IndexingToggleResponse failureSiteNotAdded() {
        return createFailureResponse("This website was not added to the site list");
    }

    public static IndexingToggleResponse failurePageUnavailable(String fullUrl) {
        return createFailureResponse("Failed to index page, " + fullUrl + " unavailable");
    }

    public static IndexingToggleResponse failureNoIndexingRunning() {
        return createFailureResponse("No indexing is running");
    }

    public static IndexingToggleResponse failureIndexingAlreadyStarted() {
        return createFailureResponse("Indexing already started");
    }

    public static IndexingToggleResponse failedToCompleteIndexingTasks() {
        return createFailureResponse("Failed to complete indexing tasks");
    }

    public static IndexingToggleResponse failedToGetIndexingTasksResult() {
        return createFailureResponse("Failed to get indexing tasks result");
    }
}
