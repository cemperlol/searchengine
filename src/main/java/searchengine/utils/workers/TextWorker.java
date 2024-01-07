package searchengine.utils.workers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextWorker {

    private static final Pattern PATTERN = Pattern.compile("[а-яё]+");

    public static List<String> findAllWords(String text) {
        text = makeTextValid(text);
        List<String> res = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            res.add(text.substring(matcher.start(), matcher.end()));
        }
        return res;
    }

    public static String makeTextValid(String text) {
        return text.toLowerCase().replaceAll("[^а-яё\\s]+", " ").replaceAll("ё", "е");
    }
}
