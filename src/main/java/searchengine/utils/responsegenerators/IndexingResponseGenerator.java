package searchengine.utils.responsegenerators;

import searchengine.dto.indexing.IndexingStatusResponse;

public class IndexingResponseGenerator {

    public static IndexingStatusResponse successResponse() {
        return new IndexingStatusResponse(true);
    }

    public static IndexingStatusResponse createFailureResponse(String errorMsg) {
        return new IndexingStatusResponse(false, errorMsg);
    }

    public static IndexingStatusResponse siteNotAdded() {
        return createFailureResponse("This website was not added to the site list");
    }

    public static IndexingStatusResponse contentUnavailable(String fullUrl) {
        return createFailureResponse("Failed to index page with url: " + fullUrl + ", content unavailable");
    }

    public static IndexingStatusResponse noIndexingRunning() {
        return createFailureResponse("No indexing is running");
    }

    public static IndexingStatusResponse indexingAlreadyStarted() {
        return createFailureResponse("Indexing already started");
    }

    public static IndexingStatusResponse userStoppedIndexing() {
        return createFailureResponse("User stopped indexing");
    }

    public static IndexingStatusResponse failedToCompleteIndexingTasks() {
        return createFailureResponse("Failed to complete indexing tasks");
    }

    public static IndexingStatusResponse failedToGetIndexingTasksResult() {
        return createFailureResponse("Failed to get indexing tasks result");
    }
}
