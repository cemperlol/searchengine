package searchengine.tests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchengine.dto.indexing.IndexingToggleResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestComponent
public class IndexingServiceTests {

    @MockBean
    private IndexingService service;

    @Test
    public void testIndexingStart() {
        IndexingToggleResponse response = IndexingResponseGenerator.successResponse();

        when(service.startIndexing()).thenReturn(response);

        IndexingToggleResponse actual = service.startIndexing();

        assertTrue(actual.isResult());
    }

    @Test
    public void testIncorrectIndexingStart() {
        IndexingToggleResponse response = IndexingResponseGenerator.indexingAlreadyStarted();

        when(service.startIndexing()).thenReturn(response);

        IndexingToggleResponse expected = IndexingResponseGenerator.indexingAlreadyStarted();
        IndexingToggleResponse actual = service.startIndexing();

        assertFalse(actual.isResult());
        assertEquals(expected.getError(), actual.getError());
    }

    @Test
    public void testStopIndexing() {
        IndexingToggleResponse response = IndexingResponseGenerator.successResponse();

        when(service.stopIndexing()).thenReturn(response);

        IndexingToggleResponse actual = service.stopIndexing();

        assertTrue(actual.isResult());
    }

    @Test
    public void testIncorrectStopIndexing() {
        IndexingToggleResponse response = IndexingResponseGenerator.noIndexingRunning();

        when(service.stopIndexing()).thenReturn(response);

        IndexingToggleResponse expected = IndexingResponseGenerator.noIndexingRunning();
        IndexingToggleResponse actual = service.stopIndexing();

        assertFalse(actual.isResult());
        assertEquals(expected.getError(), actual.getError());
    }

    @Test
    public void testIndexPage() {
        String url = "https://handmadebarbers.ru/";
        IndexingToggleResponse response = IndexingResponseGenerator.successResponse();

        when(service.indexPage(url)).thenReturn(response);

        IndexingToggleResponse actual = service.indexPage(url);

        assertTrue(actual.isResult());
    }

    @Test
    public void testIncorrectUrlIndexPage() {
        String url = "y96yuoglh9s7h";
        IndexingToggleResponse response = IndexingResponseGenerator.siteNotAdded();

        when(service.indexPage(url)).thenReturn(response);

        IndexingToggleResponse expected = IndexingResponseGenerator.siteNotAdded();
        IndexingToggleResponse actual = service.indexPage(url);

        assertFalse(actual.isResult());
        assertEquals(expected.getError(), actual.getError());
    }
}
