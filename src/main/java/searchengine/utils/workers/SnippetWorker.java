package searchengine.utils.workers;

import searchengine.model.Lemma;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SnippetWorker {

    public static String highlightLemmas(AtomicReference<String> textBlock, List<Lemma> lemmas) {
        lemmas.forEach(lemma -> {
            String regex = lemma.getLemma();
            String replacement = "<b>" + lemma.getLemma() + "</b>";
            textBlock.set(textBlock.get().replaceAll(regex, replacement));
        });

        return textBlock.get();
    }

    public static String getSnippet(List<Lemma> lemmas, String text) {
        List<AtomicReference<String>> textBlocks = Arrays.stream(text.split("([.?!]+[\\s]+)"))
                .map(AtomicReference::new)
                .toList();

        String rarestLemma = lemmas.get(0).getLemma();
        textBlocks.forEach(block -> block.set(highlightLemmas(block, lemmas)));
        StringBuilder allocatedText = new StringBuilder();
        textBlocks.forEach(block ->
                allocatedText.append(allocatedText.isEmpty() ? "" : " ").append(block.get()));

        int start = getSnippetStart(allocatedText, rarestLemma);

        return (start == 0 ? "" : "...")
                .concat(allocatedText.substring(start, Math.min(allocatedText.length(), start + 216)))
                .concat("...");
    }

    private static int getSnippetStart(StringBuilder allocatedText, String rarestLemma) {
        int start = allocatedText.toString().indexOf(rarestLemma);

        return start < 22 ? 0 : start - 22;
    }
}
