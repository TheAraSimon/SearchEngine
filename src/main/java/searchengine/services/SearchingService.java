package searchengine.services;

import searchengine.dto.searching.SearchingResponse;

import java.util.List;

public interface SearchingService {
    SearchingResponse search(String query, List<String> sitesList, int offset, int limit);
    List<String> getSiteUrlList();
}
