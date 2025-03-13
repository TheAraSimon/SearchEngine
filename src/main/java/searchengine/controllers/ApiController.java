package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.api.SearchingService;
import searchengine.services.api.SiteIndexingService;
import searchengine.services.api.StatisticsService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SiteIndexingService siteIndexingService;
    private final SearchingService searchingService;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing() {
        return siteIndexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing() {
        return siteIndexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public IndexingResponse indexPage(@RequestParam String url) {
        return siteIndexingService.indexPage(url);
    }

    @GetMapping("/search")
    public SearchingResponse search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        List<String> sitesList = (site == null) ? searchingService.getSiteUrlList() : new ArrayList<>(List.of(site));
        return searchingService.search(query, sitesList, offset, limit);
    }
}
