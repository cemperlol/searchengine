package searchengine.utils.handlers;

import lombok.RequiredArgsConstructor;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.utils.parsers.WebsiteParser;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

@RequiredArgsConstructor
public class ParsingTaskResultHandler {

    private List<CompletableFuture<ForkJoinTask<IndexingStatusResponse>>> futureTasks;

    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    private final List<WebsiteParser> tasks;

    public IndexingStatusResponse handleTasksResult() {
        try {
            return processTasks();
        } finally {
            forkJoinPool.shutdown();
        }
    }

    private IndexingStatusResponse processTasks() {
        futureTasks = new ArrayList<>();
        tasks.forEach(t -> futureTasks.add(CompletableFuture.supplyAsync(t::fork, forkJoinPool)));

        CompletableFuture<Void> completedTasks = waitForTasksToComplete();
        if (WebsiteParser.isParsingStopped()) return IndexingResponseGenerator.userStoppedIndexing();
        if (completedTasks == null) return IndexingResponseGenerator.failedToCompleteIndexingTasks();

        List<IndexingStatusResponse> results = getTasksResult();
        if (results == null) return IndexingResponseGenerator.failedToGetIndexingTasksResult();

        if (results.stream().anyMatch(IndexingStatusResponse::isResult))
            return IndexingResponseGenerator.successResponse();

        StringJoiner totalError = new StringJoiner(", " + System.lineSeparator());
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

    private List<IndexingStatusResponse> getTasksResult() {
        List<IndexingStatusResponse> results = new ArrayList<>();

        for (CompletableFuture<ForkJoinTask<IndexingStatusResponse>> task : futureTasks) {
            try {
                results.add(task.join().get());
            } catch (InterruptedException | ExecutionException e) {
                return null;
            }
        }

        return results;
    }
}
