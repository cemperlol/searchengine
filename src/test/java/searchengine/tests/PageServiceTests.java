package searchengine.tests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchengine.dto.page.PageResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.page.PageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestComponent
public class PageServiceTests {

    @MockBean
    PageService service;

    @Test
    public void testSave() {
        Site site = new Site();
        PageResponse pageResponse = new PageResponse();
        Page page = new Page();
        page.setPath("/");

        when(service.save(pageResponse, site)).thenReturn(page);

        String expected = "/";
        String actual = page.getPath();

        assertEquals(expected, actual);
    }

    @Test
    public void testGetByPathAndSiteId() {
        int expectedSiteId = 0;
        String expectedPath = "/";
        Site site = new Site();
        site.setId(expectedSiteId);
        Page page = new Page();
        page.setSite(site);
        page.setPath(expectedPath);

        when(service.getByPathAndSiteId(expectedPath, expectedSiteId)).thenReturn(page);

        Page actualPage = service.getByPathAndSiteId(expectedPath, expectedSiteId);
        int actualSiteId = actualPage.getSite().getId();
        String actualPath = actualPage.getPath();

        assertEquals(expectedSiteId, actualSiteId);
        assertEquals(expectedPath, actualPath);
    }

    @Test void testGetByIncorrectPath() {
        int siteId = 0;
        String path = "08249082903472038";

        when(service.getByPathAndSiteId(path, siteId)).thenReturn(null);

        Page actual = service.getByPathAndSiteId(path, siteId);

        assertNull(actual);
    }
}
