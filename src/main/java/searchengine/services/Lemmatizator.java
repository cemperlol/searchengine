package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.Site;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Lemmatizator {

    private static final LuceneMorphology russianLuceneMorph = getRussianMorphology();

    public static Map<String, Integer> getLemmas(Site site, Document doc) {
        if (russianLuceneMorph == null) return null; //TODO: add lemmatizator exceptions
        Map<String, Integer> lemmas = new HashMap<>();
        String text = clearFromHtml(doc);
        Matcher matcher = Pattern.compile("([а-яё]+[.]?[а-яё]+)+").matcher(text);

        while(matcher.find()) {
            String word = text.substring(matcher.start(), matcher.end());

            List<String> wordNormalForms = russianLuceneMorph.getNormalForms(word).stream()
                    .filter(f -> !checkIfServicePart(f)).toList();
            wordNormalForms.forEach(lemma -> lemmas.put(lemma, 1));
        }

        return lemmas;
    }

    public static String clearFromHtml(Document doc) {
        return doc.wholeText().toLowerCase();
    }

    public static void saveLemmas(List<String> lemmas) {

    }

    private static boolean checkIfServicePart(String wordForm) {
        return wordForm.contains("МЕЖД") || wordForm.contains("СОЮЗ") ||
                wordForm.contains("ЧАСТ") || wordForm.contains("ПРЕДЛ");
    }

    private static RussianLuceneMorphology getRussianMorphology() {
        try {
            return new RussianLuceneMorphology();
        } catch (IOException e) {
            ApplicationLogger.log(e);
        }

        return null;
    }
}
