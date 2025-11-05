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
        StringBuilder highlightedText = new StringBuilder();
        textBlocks.forEach(block ->
                highlightedText.append(highlightedText.isEmpty() ? "" : " ").append(block.get()));

        int start = getSnippetStart(highlightedText);
        
        return (start == 0 ? "" : "...")
                .concat(highlightedText.substring(start, Math.min(highlightedText.length(), start + 216)))
                .concat("...");
    }

    private static int getSnippetStart(StringBuilder highlightedText) {
        int start = highlightedText.toString().indexOf("<b>");

        return start < 22 ? 0 : start - 22;
    }
}
