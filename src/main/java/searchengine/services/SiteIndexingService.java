package searchengine.services;

import searchengine.dto.statistics.IndexingResponse;

public interface SiteIndexingService {
    IndexingResponse startIndexing();
}
