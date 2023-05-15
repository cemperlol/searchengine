package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.model.Site;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Lemmatizator {

    public static void getLemmas(Site site, String text) {
        LuceneMorphology luceneMorph = getRussianMorphology();
        if (luceneMorph == null) return;
        text = text.toLowerCase(Locale.ROOT);
        Matcher matcher = Pattern.compile("а-яё+");
        luceneMorph.getNormalForms();
    }

    public static void saveLemmas(List<String> lemmas) {

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
