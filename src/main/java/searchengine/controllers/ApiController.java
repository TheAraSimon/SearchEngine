package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.SearchingService;
import searchengine.services.SiteIndexingService;
import searchengine.services.StatisticsService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SiteIndexingService siteIndexingService;
    private final SearchingService searchingService;


    public ApiController(StatisticsService statisticsService,
                         SiteIndexingService siteIndexingService,
                         SearchingService searchingService) {
        this.statisticsService = statisticsService;
        this.siteIndexingService = siteIndexingService;
        this.searchingService = searchingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(siteIndexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(siteIndexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(siteIndexingService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchingResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        List<String> sitesList = (site == null) ? searchingService.getSiteUrlList() : new ArrayList<>(List.of(site));
        return ResponseEntity.ok(searchingService.search(query, sitesList));
    }
}
