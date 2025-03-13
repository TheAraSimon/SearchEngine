package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.SiteDto;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.services.api.StatisticsService;
import searchengine.services.repositoryServices.LemmaCRUDService;
import searchengine.services.repositoryServices.PageCRUDService;
import searchengine.services.repositoryServices.SiteCRUDService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final PageCRUDService pageCRUDService;
    private final SiteCRUDService siteCRUDService;
    private final LemmaCRUDService lemmaCRUDService;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        List<DetailedStatisticsItem> detailed = new CopyOnWriteArrayList<>();
        total.setSites(siteCRUDService.countNumberOfSitesInDB());
        total.setIndexing(true);

        List<Site> sitesList = siteCRUDService.getAllSites();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            sitesList.forEach(site -> executorService.submit(() -> {
                DetailedStatisticsItem item = new DetailedStatisticsItem();
                item.setName(site.getName());
                item.setUrl(site.getUrl());

                SiteDto siteDto = siteCRUDService.getByUrl(site.getUrl());
                item.setStatus(siteDto.getStatus().toString());
                long statusTimeInSeconds = siteDto.getStatusTime().getEpochSecond() * 1000;
                item.setStatusTime(statusTimeInSeconds);
                item.setError(siteDto.getLastError());

                int siteId = siteDto.getId();
                int pages = pageCRUDService.getPageCountBySiteId(siteId);
                int lemmas = lemmaCRUDService.getLemmaCountBySiteId(siteId);

                item.setPages(pages);
                item.setLemmas(lemmas);

                synchronized (total) {
                    total.setPages(total.getPages() + pages);
                    total.setLemmas(total.getLemmas() + lemmas);
                }

                detailed.add(item);
            }));
        } catch (Exception e) {
            log.error("Error processing site statistics", e);
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
                        log.error("The thread pool did not complete its work within the timeout period");
                    }
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (detailed.stream().noneMatch(item -> "INDEXING".equals(item.getStatus()))) {
            total.setIndexing(false);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
