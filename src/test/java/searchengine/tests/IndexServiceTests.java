package searchengine.tests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.services.index.IndexService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestComponent
public class IndexServiceTests {

    @MockBean
    IndexService service;

    @Test
    public void testSave() {
        int expectedPageId = 0;
        int expectedLemmaId = 0;
        int expectedRank = 5;

        Index index = new Index();
        Lemma lemma = new Lemma();
        lemma.setId(expectedLemmaId);
        Page page = new Page();
        page.setId(expectedPageId);
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(expectedRank);

        when(service.save(page, lemma, expectedRank)).thenReturn(index);

        Index actual = service.save(page, lemma, expectedRank);

        assertEquals(expectedPageId, actual.getPage().getId());
        assertEquals(expectedLemmaId, actual.getLemma().getId());
        assertEquals(expectedRank, (int) actual.getRank());
    }

    @Test
    public void testGetByPageId() {
        int expected = 0;

        List<Index> indexes = new ArrayList<>();
        Index index = new Index();
        Page page = new Page();
        page.setId(expected);
        index.setPage(page);
        indexes.add(index);

        when(service.getByPageId(expected)).thenReturn(indexes);

        indexes.forEach(i -> assertEquals(expected, i.getPage().getId()));
    }

    @Test
    public void getLemmasByPageId() {
        int expected = 5;

        int pageId = 0;
        List<Lemma> lemmas = new ArrayList<>();
        for (int i = 0; i < expected; i++) lemmas.add(new Lemma());

        when(service.getLemmasByPageId(pageId)).thenReturn(lemmas);

        int actual = service.getLemmasByPageId(pageId).size();

        assertEquals(expected, actual);
    }
}
