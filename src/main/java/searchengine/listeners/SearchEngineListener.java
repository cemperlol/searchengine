package searchengine.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import searchengine.services.site.SiteService;
import searchengine.services.site.SiteServiceImpl;

@Component
public class SearchEngineListener implements ApplicationListener<ApplicationReadyEvent> {

    private final SiteService siteService;

    @Autowired
    public SearchEngineListener(SiteService siteService) {
        this.siteService = siteService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        siteService.getAll().forEach(siteService::updateIncorrectShutdown);
    }
}
