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

    public static String highlightClosestToLemma(StringBuilder text, String rarestLemma) {
        String result = "";

        int start = text.toString().indexOf(rarestLemma.substring(0, rarestLemma.length() / 2));
        int end = text.toString().indexOf(" ", start);
        result = result.concat(text.substring(0, start)).concat("<b>");
        result = result.concat(text.substring(start, end)).concat("</b>");

        return result.concat(text.substring(end));
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

        return "..."
                .concat(allocatedText.substring(start, Math.min(allocatedText.length(), start + 216)))
                .concat("...");
    }

    private static int getSnippetStart(StringBuilder allocatedText, String rarestLemma) {
        int start;

        if (!allocatedText.toString().contains("<b>")) {
            allocatedText.replace(0, allocatedText.length(), highlightClosestToLemma(allocatedText, rarestLemma));
            start = allocatedText.toString().indexOf(rarestLemma.substring(0, rarestLemma.length() / 2 + 1));
        } else {
            start = allocatedText.toString().indexOf(rarestLemma);
        }

        return start < 22 ? 0 : start - 22;
    }
}
