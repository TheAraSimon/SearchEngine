package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface SiteIndexingService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
}
