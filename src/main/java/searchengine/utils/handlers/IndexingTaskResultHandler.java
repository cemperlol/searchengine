package searchengine.utils.handlers;

import lombok.RequiredArgsConstructor;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.services.indexing.AbstractIndexingService;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
public class IndexingTaskResultHandler {

    private List<CompletableFuture<IndexingToggleResponse>> futureTasks;

    private final List<AbstractIndexingService> tasks;

    public IndexingToggleResponse HandleTasksResult() {
        futureTasks = new ArrayList<>();
        tasks.forEach(t -> futureTasks.add(CompletableFuture.supplyAsync(t::compute)));

        CompletableFuture<Void> completedTasks = waitForTasksToComplete();
        if (tasks.stream().anyMatch(AbstractIndexingService::getIndexingStopped))
            return IndexingResponseGenerator.userStoppedIndexing();
        if (completedTasks == null) return IndexingResponseGenerator.failedToCompleteIndexingTasks();

        List<IndexingToggleResponse> results = getTasksResult();
        if (results == null) return IndexingResponseGenerator.failedToGetIndexingTasksResult();

        if (results.stream().noneMatch(result -> result.getError().equals("User stopped indexing")) &&
                results.stream().anyMatch(IndexingToggleResponse::isResult)) {
            return IndexingResponseGenerator.successResponse();
        }

        StringJoiner totalError = new StringJoiner(", ");
        results.forEach(r -> {
            if (!r.isResult()) totalError.add(r.getError());
        });
        return IndexingResponseGenerator.createFailureResponse(totalError.toString());
    }

    private CompletableFuture<Void> waitForTasksToComplete() {
        CompletableFuture<Void> completedTasks =
                CompletableFuture.allOf(futureTasks.toArray(new CompletableFuture[0]));

        try {
            completedTasks.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }

        return completedTasks;
    }

    private List<IndexingToggleResponse> getTasksResult() {
        List<IndexingToggleResponse> results = new ArrayList<>();

        for (CompletableFuture<IndexingToggleResponse> task : futureTasks) {
            try {
                results.add(task.get());
            } catch (InterruptedException | ExecutionException e) {
                return null;
            }
        }

        return results;
    }
}
