package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchData;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.statistics.DetailedStatisticsItem;

import java.util.List;

public class SearchingResponser {
    public SearchingResponser() {
    }

    public SearchingResponse createErrorResponse(String message) {
        SearchingResponse response = new SearchingResponse();
        response.setError(message);
        response.setResult(false);
        return response;
    }

    public SearchingResponse createSuccessfulResponse(int count, List<SearchData> data) {
        SearchingResponse response = new SearchingResponse();
        response.setCount(count);
        response.setData(data);
        response.setResult(true);
        return response;
    }
}
