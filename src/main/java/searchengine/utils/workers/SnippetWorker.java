package searchengine.utils.workers;

import searchengine.utils.lemmas.Lemmatizator;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class SnippetWorker {



    public static String highlightLemmas(AtomicReference<String> textBlock, Set<String> words) {
        words.forEach(word -> {
            for (String textBlockWord : TextWorker.findAllWords(textBlock.get())) {
                String blockWordForm = Lemmatizator.getLemmas(textBlockWord).keySet().stream()
                        .findAny().orElse("");
                String replacement = "<b>" + textBlockWord + "</b>";

                if (words.contains(blockWordForm)) {
                    textBlock.set(textBlock.get().replaceAll(textBlockWord, replacement));
                }
            }
        });

        return textBlock.get();
    }

    public static String getSnippet(Set<String> words, String text) {
        List<AtomicReference<String>> textBlocks = TextWorker.getTextSentencesAsAtomicReferences(text);
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
