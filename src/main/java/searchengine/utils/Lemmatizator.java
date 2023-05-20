package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;
import searchengine.services.logging.ApplicationLogger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lemmatizator {

    private static final Set<String> SERVICE_PARTS = Set.of("МЕЖД", "СОЮЗ", "ПРЕДЛ");
    private static final Pattern PATTERN = Pattern.compile("\\b[а-яё]+([-.][а-яё]+)*\\b");

    private static final LuceneMorphology russianLuceneMorph = getRussianMorphology();

    public static Map<String, Integer> getLemmas(Document doc) {
        if (doc == null || russianLuceneMorph == null) return new HashMap<>(); //TODO: add lemmatizator exceptions

        String text = clearFromHtml(doc);
        Matcher matcher = PATTERN.matcher(text);
        Map<String, Integer> lemmas = new HashMap<>();

        while (matcher.find()) {
            String word = text.substring(matcher.start(), matcher.end());
            if (word.isBlank() || checkIfServicePart(word)) continue;
            lemmas.merge(russianLuceneMorph.getNormalForms(word).get(0), 1, Integer::sum);
        }

        return lemmas;
    }

    public static String clearFromHtml(Document doc) {
        return doc.text().replaceAll("(^[а-яё]\\s)+", " ").replaceAll("ё", "е").toLowerCase();
    }

    private static boolean checkIfServicePart(String wordForm) {
        for (String servicePart : SERVICE_PARTS) {
            if (wordForm.contains(servicePart)) return true;
        }

        return false;
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
