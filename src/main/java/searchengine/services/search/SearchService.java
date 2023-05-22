package searchengine.services.search;

import searchengine.dto.search.SearchResponse;

public interface SearchService {

    SearchResponse siteSearch(String query, String site, int offset, int limit);

    SearchResponse globalSearch(String query, int offset, int limit);
}
