package searchengine.utils.lemmas;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;
import searchengine.logging.ApplicationLogger;
import searchengine.utils.workers.TextWorker;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Lemmatizator {

    private static final Set<String> SERVICE_PARTS = Set.of("МЕЖД", "СОЮЗ", "ПРЕДЛ");

    private static final LuceneMorphology russianLuceneMorph = getRussianMorphology();

    public static Map<String, Integer> getLemmas(String text) {
        if (russianLuceneMorph == null) return new HashMap<>();

        List<String> words = TextWorker.findAllWords(text);
        Map<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            russianLuceneMorph.getNormalForms(word).stream()
                    .filter(f -> !checkIfServicePart(f))
                    .forEach(f -> lemmas.merge(f, 1, Integer::sum));
        }

        return lemmas;
    }

    public static Map<String, Integer> getLemmas(Document doc) {
        return doc == null ? new HashMap<>() : getLemmas(doc.text());
    }

    private static boolean checkIfServicePart(String wordForm) {
        List<String> info = russianLuceneMorph.getMorphInfo(wordForm);
        for (String servicePart : SERVICE_PARTS) {
            if (info.stream().anyMatch(i -> i.contains(servicePart))) {
                return true;
            }
        }

        return false;
    }

    private static RussianLuceneMorphology getRussianMorphology() {
        try {
            return new RussianLuceneMorphology();
        } catch (IOException e) {
            ApplicationLogger.logError(e);
        }

        return null;
    }
}
