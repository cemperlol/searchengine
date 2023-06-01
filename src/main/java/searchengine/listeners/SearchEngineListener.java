package searchengine.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import searchengine.services.site.SiteServiceImpl;

@Component
public class SearchEngineListener implements ApplicationListener<ApplicationReadyEvent> {

    private final SiteServiceImpl siteServiceImpl;

    @Autowired
    public SearchEngineListener(SiteServiceImpl siteServiceImpl) {
        this.siteServiceImpl = siteServiceImpl;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        siteServiceImpl.getAll().forEach(siteServiceImpl::updateIncorrectShutdown);
    }
}
