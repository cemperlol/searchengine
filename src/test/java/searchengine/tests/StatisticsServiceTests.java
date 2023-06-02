package searchengine.tests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.statistics.StatisticsService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestComponent
public class StatisticsServiceTests {

    @MockBean
    private StatisticsService service;

    @Test
    public void testGetStatistics() {
        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(new StatisticsData());

        when(service.getStatistics()).thenReturn(response);

        StatisticsResponse actual = service.getStatistics();

        assertTrue(actual.isResult());
        assertNotNull(actual.getStatistics());
    }
}
