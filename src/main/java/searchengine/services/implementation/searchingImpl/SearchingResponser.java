package searchengine.services.implementation.searchingImpl;

import searchengine.dto.searching.SearchResult;
import searchengine.dto.searching.SearchingResponse;

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

    public SearchingResponse createSuccessfulResponse(int count, List<SearchResult> data) {
        SearchingResponse response = new SearchingResponse();
        response.setCount(count);
        response.setData(data);
        response.setResult(true);
        return response;
    }
}
