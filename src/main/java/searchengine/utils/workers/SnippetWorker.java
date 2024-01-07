package searchengine.utils.workers;

import searchengine.model.Lemma;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SnippetWorker {

    public static String highlightLemmas(AtomicReference<String> textBlock, List<String> words) {
        words.forEach(word -> {
            String replacement = "<b>" + word + "</b>";
            textBlock.set(textBlock.get().replaceAll(word, replacement));
        });

        return textBlock.get();
    }

    public static String getSnippet(List<String> words, String text) {
        List<AtomicReference<String>> textBlocks = Arrays.stream(text.split("([.?!]+[\\s]+)"))
                .map(AtomicReference::new)
                .toList();

        textBlocks.forEach(block -> block.set(highlightLemmas(block, words)));
        StringBuilder allocatedText = new StringBuilder();
        textBlocks.forEach(block ->
                allocatedText.append(allocatedText.isEmpty() ? "" : " ").append(block.get()));

        int start = getSnippetStart(allocatedText);

        return (start == 0 ? "" : "...")
                .concat(allocatedText.substring(start, Math.min(allocatedText.length(), start + 216)))
                .concat("...");
    }

    private static int getSnippetStart(StringBuilder allocatedText) {
        int start = allocatedText.toString().indexOf("<b>");

        return start < 22 ? 0 : start - 22;
    }
}
