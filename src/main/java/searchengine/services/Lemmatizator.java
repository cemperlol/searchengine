package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public abstract class Lemmatizator {

    private static final LuceneMorphology russianLuceneMorph = getRussianMorphology();

    private static LemmaService lemmaService;

    @Autowired
    public static void setLemmaService(LemmaService lemmaService) {
        Lemmatizator.lemmaService = lemmaService;
    }


    public static Map<Lemma, Integer> getLemmas(Site site, Document doc) {
        if (russianLuceneMorph == null) return null; //TODO: add lemmatizator exceptions
        Map<Lemma, Integer> lemmasAndFrequency = new HashMap<>();
        String text = clearFromHtml(doc);
        Matcher matcher = Pattern.compile("([а-яё]+[.]?[а-яё]+)+").matcher(text);

        List<String> wordNormalForms = new ArrayList<>();
        while(matcher.find()) {
            String word = text.substring(matcher.start(), matcher.end());

             wordNormalForms.addAll(russianLuceneMorph.getNormalForms(word).stream()
                     .filter(f -> !checkIfServicePart(f)).distinct().toList());
        }
        List<Lemma> lemmas = lemmaService.saveAllLemmas(wordNormalForms.stream().distinct().toList(), site);
        wordNormalForms.forEach(lemmaValue -> {
            Lemma lemma = lemmas.stream().filter(l -> l.getLemma().equals(lemmaValue)).findFirst().orElse(null);
            if (lemma != null)
                lemmasAndFrequency.put(lemma, lemmasAndFrequency.getOrDefault(lemma, 1) + 1);
        });

        return lemmasAndFrequency;
    }

    public static String clearFromHtml(Document doc) {
        return doc.wholeText().toLowerCase();
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
