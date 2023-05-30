package searchengine.utils.workers;

import searchengine.model.Index;

import java.util.Collection;
import java.util.Set;
import java.util.stream.DoubleStream;

public class RelevanceWorker {

    public static float getPageRelevance(Set<Index> indexes) {
        return (float) indexes.stream()
                .flatMapToDouble(index -> DoubleStream.of(index.getRank()))
                .sum();
    }

    public static float getAbsRelevance(Collection<Set<Index>> indexesList) {
        return indexesList.stream()
                .map(RelevanceWorker::getPageRelevance)
                .max(Double::compare).orElse(1.0f);
    }

    public static float getRelRelevance(Set<Index> indexes, float absRelevance) {
        float pageRelevance = getPageRelevance(indexes);
        return pageRelevance / absRelevance;
    }
}
