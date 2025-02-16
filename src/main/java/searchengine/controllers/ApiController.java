package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchingResponse;
import searchengine.exceptions.ApiException;
import searchengine.services.api.SearchingService;
import searchengine.services.api.SiteIndexingService;
import searchengine.services.api.StatisticsService;

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
    public ResponseEntity<?> statistics() {
        try {
            return ResponseEntity.ok(statisticsService.getStatistics());
        } catch (Exception e) {
            throw new ApiException("Ошибка при получении статистики", 500);
        }
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        try {
            IndexingResponse response = siteIndexingService.startIndexing();
            if (!response.isResult()) {
                throw new ApiException(response.getError(), 400);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ApiException("Ошибка при запуске индексации", 500);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        try {
            IndexingResponse response = siteIndexingService.stopIndexing();
            if (!response.isResult()) {
                throw new ApiException(response.getError(), 400);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ApiException("Ошибка при остановке индексации", 500);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                throw new ApiException("URL страницы не может быть пустым", 400);
            }

            IndexingResponse response = siteIndexingService.indexPage(url);
            if (!response.isResult()) {
                throw new ApiException(response.getError(), 400);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ApiException("Ошибка при индексации страницы", 500);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        try {
            if (query == null || query.trim().isEmpty()) {
                throw new ApiException("Поисковый запрос не может быть пустым", 400);
            }
            List<String> sitesList = (site == null) ? searchingService.getSiteUrlList() : new ArrayList<>(List.of(site));
            SearchingResponse response = searchingService.search(query, sitesList, offset, limit);
            if (!response.isResult()) {
                throw new ApiException(response.getError(), 404);
            }
            return ResponseEntity.ok(response);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Ошибка при выполнении поиска", 500);
        }
    }
}
