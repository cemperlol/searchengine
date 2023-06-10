package searchengine.services;

import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Map;

public interface ParsingSubscriber {
    void update(Site site, Page page, Map<String, Integer> lemmasAndFrequency);
}
