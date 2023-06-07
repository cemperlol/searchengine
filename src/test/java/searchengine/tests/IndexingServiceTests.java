package searchengine.tests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
public class IndexingServiceTests {

    @MockBean
    private IndexingService service;

    @Test
    public void testIndexingStart() {
        IndexingStatusResponse response = IndexingResponseGenerator.successResponse();

        when(service.startIndexing()).thenReturn(response);

        IndexingStatusResponse actual = service.startIndexing();

        assertTrue(actual.isResult());
    }

    @Test
    public void testIncorrectIndexingStart() {
        IndexingStatusResponse response = IndexingResponseGenerator.indexingAlreadyStarted();

        when(service.startIndexing()).thenReturn(response);

        IndexingStatusResponse expected = IndexingResponseGenerator.indexingAlreadyStarted();
        IndexingStatusResponse actual = service.startIndexing();

        assertFalse(actual.isResult());
        assertEquals(expected.getError(), actual.getError());
    }

    @Test
    public void testStopIndexing() {
        IndexingStatusResponse response = IndexingResponseGenerator.successResponse();

        when(service.stopIndexing()).thenReturn(response);

        IndexingStatusResponse actual = service.stopIndexing();

        assertTrue(actual.isResult());
    }

    @Test
    public void testIncorrectStopIndexing() {
        IndexingStatusResponse response = IndexingResponseGenerator.noIndexingRunning();

        when(service.stopIndexing()).thenReturn(response);

        IndexingStatusResponse expected = IndexingResponseGenerator.noIndexingRunning();
        IndexingStatusResponse actual = service.stopIndexing();

        assertFalse(actual.isResult());
        assertEquals(expected.getError(), actual.getError());
    }

    @Test
    public void testIndexPage() {
        String url = "https://handmadebarbers.ru/";
        IndexingStatusResponse response = IndexingResponseGenerator.successResponse();

        when(service.indexPage(url)).thenReturn(response);

        IndexingStatusResponse actual = service.indexPage(url);

        assertTrue(actual.isResult());
    }

    @Test
    public void testIncorrectUrlIndexPage() {
        String url = "y96yuoglh9s7h";
        IndexingStatusResponse response = IndexingResponseGenerator.siteNotAdded();

        when(service.indexPage(url)).thenReturn(response);

        IndexingStatusResponse expected = IndexingResponseGenerator.siteNotAdded();
        IndexingStatusResponse actual = service.indexPage(url);

        assertFalse(actual.isResult());
        assertEquals(expected.getError(), actual.getError());
    }
}
