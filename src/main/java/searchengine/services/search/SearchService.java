package searchengine.services.search;

import searchengine.dto.search.SearchResponse;

public interface SearchService {

    SearchResponse siteSearch(String query, String site);

    SearchResponse globalSearch(String query);
}
