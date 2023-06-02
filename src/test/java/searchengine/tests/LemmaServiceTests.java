package searchengine.tests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.services.lemma.LemmaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@SpringBootTest
public class LemmaServiceTests {

    @MockBean
    LemmaService service;

    @Test
    public void testSave() {
        String expectedLemmaValue = "что";
        int expectedSiteId = 0;

        Lemma lemma = new Lemma();
        Site site = new Site();
        site.setId(expectedSiteId);
        lemma.setSite(site);
        lemma.setLemma(expectedLemmaValue);

        when(service.save(site, expectedLemmaValue)).thenReturn(lemma);

        Lemma actual = service.save(site, expectedLemmaValue);

        assertEquals(expectedSiteId, actual.getSite().getId());
        assertEquals(expectedLemmaValue, actual.getLemma());
    }

    @Test
    public void testSaveAll() {
        int expected = 3;

        List<Lemma> lemmas = new ArrayList<>();
        List<String> lemmaValues = new ArrayList<>();
        String lemmaValue = "";
        Lemma lemma = new Lemma();
        Site site = new Site();

        for (int i = 0; i < expected; i++) {
            lemmas.add(lemma);
            lemmaValues.add(lemmaValue);
        }

        when(service.saveAll(lemmaValues, site)).thenReturn(lemmas);

        int actual = service.saveAll(lemmaValues, site).size();

        assertEquals(expected, actual);
    }

    @Test
    public void testGetByLemmaAndSiteId() {
        int expectedSiteId = 0;
        String expectedLemmaValue = "что";

        Lemma lemma = new Lemma();
        Site site = new Site();
        site.setId(expectedSiteId);
        lemma.setLemma(expectedLemmaValue);
        lemma.setSite(site);

        when(service.getByLemmaAndSiteId(expectedLemmaValue, expectedSiteId)).thenReturn(lemma);

        Lemma actual = service.getByLemmaAndSiteId(expectedLemmaValue, expectedSiteId);

        assertEquals(expectedSiteId, actual.getSite().getId());
        assertEquals(expectedLemmaValue, actual.getLemma());
    }

    @Test
    public void testGetByIncorrectLemma() {
        int siteId = 0;
        String incorrectLemmaValue = "lfdjs";

        when(service.getByLemmaAndSiteId(incorrectLemmaValue, siteId)).thenReturn(null);

        Lemma actual = service.getByLemmaAndSiteId(incorrectLemmaValue, siteId);

        assertNull(actual);
    }

    @Test
    public void testFilterTooFrequentLemmas() {
        int pageCount = 100;
        Random random = new Random();
        Lemma lemma = new Lemma();
        lemma.setFrequency(random.nextInt(0, 100));

        when(service.filterTooFrequentLemmasOnSite(lemma, pageCount))
                .thenReturn((float) lemma.getFrequency() / pageCount >= .25f);

        boolean expected = (float) lemma.getFrequency() / pageCount >= .25f;
        boolean actual = service.filterTooFrequentLemmasOnSite(lemma, pageCount);

        assertEquals(expected, actual);
    }
}
