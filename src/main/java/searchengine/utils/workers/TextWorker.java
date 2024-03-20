package searchengine.utils.workers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextWorker {

    private static final Pattern WORD_PATTERN = Pattern.compile("[а-яё]+");

    public static List<String> findAllWords(String text) {
        text = makeTextValid(text);
        List<String> res = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            res.add(text.substring(matcher.start(), matcher.end()));
        }
        return res;
    }

    public static String makeTextValid(String text) {
        return text.toLowerCase().replaceAll("[^а-яё\\s]+", " ").replaceAll("ё", "е");
    }

    public static List<AtomicReference<String>> getTextSentencesAsAtomicReferences(String text) {
        return Arrays.stream(text.split("[.?!]+[\s]+"))
                .map(AtomicReference::new)
                .toList();
    }
}
