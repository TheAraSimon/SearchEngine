package searchengine.services;

import searchengine.dto.indexing.SiteDto;
import searchengine.dto.searching.SearchingResponse;

import java.util.List;

public interface SearchingService {
    SearchingResponse search(String query, List<String> sitesList);
    List<String> getSiteUrlList();
}
