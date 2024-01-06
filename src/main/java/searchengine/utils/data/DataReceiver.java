package searchengine.utils.data;

import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Map;

public interface DataReceiver {

    void receiveData(Site site, Page page, Map<String, Integer> lemmasAndFrequencies);
}
