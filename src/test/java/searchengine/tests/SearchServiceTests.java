package searchengine.tests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchServiceResult;
import searchengine.services.search.SearchService;
import searchengine.utils.responsegenerators.SearchResponseGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
public class SearchServiceTests {

    @MockBean
    private SearchService service;

    @Test
    public void testSearch() {
        int expected = 11;

        String query = "какой-то запрос";
        String siteUrl = "https://some.url";
        int limit = 10;
        int offset = 0;

        SearchServiceResult[] results = new SearchServiceResult[expected];
        for (int i = 0; i < results.length; i++) results[i] = new SearchServiceResult();

        SearchResponse response = new SearchResponse();
        response.setData(results);
        response.setResult(true);
        response.setCount(expected);

        when(service.search(query, siteUrl, limit, offset)).thenReturn(response);

        SearchResponse actual = service.search(query, siteUrl, limit, offset);

        assertTrue(actual.isResult());
        assertEquals(expected, actual.getData().length);
    }

    @Test
    public void testIncorrectQuerySearch() {
        SearchResponse response = SearchResponseGenerator.emptyQuery();
        String siteUrl = "https://some.url";
        int limit = 10;
        int offset = 0;

        when(service.search("", siteUrl, limit, offset)).thenReturn(response);

        String expected = response.getError();
        String actual = service.search("", siteUrl, limit, offset).getError();

        assertEquals(expected, actual);
    }
}
