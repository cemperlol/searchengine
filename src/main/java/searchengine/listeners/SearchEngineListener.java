package searchengine.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;

@Component
public class SearchEngineListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final String INCORRECT_SHUTDOWN = "Application was shutdown incorrectly";

    private final SiteRepository siteRepository;

    @Autowired
    public SearchEngineListener(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        siteRepository.findAll().stream()
                .filter(site -> site.getStatus().equals(SiteStatus.INDEXING))
                .forEach(site -> siteRepository.updateLastError(site.getId(), INCORRECT_SHUTDOWN));
    }
}
