package searchengine.utils.handlers;

import lombok.RequiredArgsConstructor;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.utils.parsers.WebsiteParser;
import searchengine.utils.responsegenerators.IndexingResponseGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;

@RequiredArgsConstructor
public class ParsingTaskResultHandler {

    private List<ForkJoinTask<IndexingStatusResponse>> futureTasks;

    private final List<WebsiteParser> tasks;

    public IndexingStatusResponse handleTasksResult() {
        futureTasks = new ArrayList<>();
        tasks.forEach(t -> futureTasks.add(t.fork()));

        tasks.forEach(ForkJoinTask::join);

        List<IndexingStatusResponse> results = getTasksResult();
        if (results == null) {
            return WebsiteParser.isParsingStopped() ?
                    IndexingResponseGenerator.userStoppedIndexing() :
                    IndexingResponseGenerator.failedToGetIndexingTasksResult();
        }

        if (results.stream().allMatch(IndexingStatusResponse::isResult))
            return IndexingResponseGenerator.successResponse();

        StringJoiner errorsList = new StringJoiner(";\t");
        results.forEach(r -> {
            if (!r.isResult() && !errorsList.toString().contains(r.getError())) {
                errorsList.add(r.getError());
            }
        });
        return IndexingResponseGenerator.createFailureResponse(errorsList.toString());
    }

    private List<IndexingStatusResponse> getTasksResult() {
        List<IndexingStatusResponse> results = new ArrayList<>();

        for (ForkJoinTask<IndexingStatusResponse> task : futureTasks) {
            try {
                results.add(task.get());
            } catch (InterruptedException | ExecutionException e) {
                return null;
            }
        }

        return results;
    }
}
