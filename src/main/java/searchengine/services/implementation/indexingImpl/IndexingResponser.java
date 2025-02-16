package searchengine.services.implementation.indexingImpl;

import searchengine.dto.indexing.IndexingResponse;

public class IndexingResponser {
    public IndexingResponser() {
    }

    public IndexingResponse createErrorResponse(String message) {
        IndexingResponse response = new IndexingResponse();
        response.setError(message);
        response.setResult(false);
        return response;
    }

    public IndexingResponse createSuccessfulResponse() {
        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }
}
