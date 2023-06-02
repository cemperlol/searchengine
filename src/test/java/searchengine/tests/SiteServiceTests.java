package searchengine.tests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.services.site.SiteService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestComponent
public class SiteServiceTests {

    @MockBean
    private SiteService service;

    private final List<searchengine.config.Site> configSites = createSomeConfigSites();

    @Test
    public void testSave() {
        Site site = new Site();
        site.setName("PlayBack.Ru");

        when(service.save(configSites.get(0))).thenReturn(site);

        String expected = "PlayBack.Ru";
        String actual = service.save(configSites.get(0)).getName();

        assertEquals(expected, actual);
    }

    @Test
    public void testGetByUrl() {
        String url = "https://playback.ru";
        Site site = new Site();
        site.setUrl(url);

        when(service.getByUrl("url")).thenReturn(site);

        Site actual = service.getByUrl("url");

        assertNotNull(actual);
    }

    @Test
    public void testGetByIncorrectUrl() {
        String url = "r832748923";

        when(service.getByUrl(url)).thenReturn(null);

        Site actual = service.getByUrl(url);

        assertNull(actual);
    }

    @Test
    public void testGetAll() {
        int expected = configSites.size();

        List<Site> sites = new ArrayList<>();
        Site site = new Site();
        for (int i = 0; i < expected; i++) sites.add(site);

        when(service.save(configSites.get(0))).thenReturn(site);
        when(service.getAll()).thenReturn(sites);

        for (int i = 0; i < configSites.size(); i++) service.save(configSites.get(0));

        int actual = service.getAll().size();

        assertEquals(expected, actual);
    }

    @Test
    public void testUpdateSiteLastError() {
        Site site = new Site();
        site.setStatus(SiteStatus.FAILED);
        site.setLastError("lmr");

        when(service.updateLastError(0, "lmr")).thenReturn(site);

        Site expected = new Site();
        expected.setStatus(SiteStatus.FAILED);
        expected.setLastError("lmr");

        Site actual = service.updateLastError(0, "lmr");

        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getLastError(), actual.getLastError());
    }

    private List<searchengine.config.Site> createSomeConfigSites() {
        List<searchengine.config.Site> configSites = new ArrayList<>();

        searchengine.config.Site site1 = new searchengine.config.Site();
        site1.setName("PlayBack.Ru");
        site1.setUrl("https://www.playback.ru");
        configSites.add(site1);

        searchengine.config.Site site2 = new searchengine.config.Site();
        site1.setName("HandmadeBarbers");
        site1.setUrl("https://www.handmadebarbers.ru");
        configSites.add(site2);

        searchengine.config.Site site3 = new searchengine.config.Site();
        site1.setName("Огонек");
        site1.setUrl("https://ogonek-rest.ru");
        configSites.add(site3);

        return configSites;
    }
}
