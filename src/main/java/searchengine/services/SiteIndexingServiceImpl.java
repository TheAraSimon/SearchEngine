package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionProfile;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Status;
import searchengine.services.repositoryServices.PageCRUDService;
import searchengine.services.repositoryServices.SiteCRUDService;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class SiteIndexingServiceImpl implements SiteIndexingService {
    private final SitesList sites;
    private final ConnectionProfile connectionProfile;
    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;

    private ExecutorService executorService;
    private final AtomicBoolean isIndexing = new AtomicBoolean(false);

    @Override
    @Transactional
    public IndexingResponse startIndexing() {
        if (isIndexing.get()) {
            return createErrorResponse("Indexing is in process");
        }

        List<Site> siteList = sites.getSites().stream().distinct().toList();
        if (siteList.isEmpty()) {
            return createErrorResponse("Sites list is empty");
        }

        isIndexing.set(true);
        SiteMapper.requestStart();
        SiteMapper.clearVisitedUrls();

        if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }

        for (Site site : siteList) {
            executorService.submit(() -> indexSite(site));
        }

        executorService.shutdown();
        //TODO: add logging
        return createSuccessfulResponse();
    }

    @Override
    @Transactional
    public IndexingResponse stopIndexing() {
        if (!isIndexing.get()) {
            //TODO: delete sout and add logging
            System.out.println("Индексация не запущена");
            return createErrorResponse("Индексация не запущена");
        } else {
            SiteMapper.requestStop();
            executorService.shutdownNow();
            isIndexing.set(false);
            //TODO: delete sout and add logging
            System.out.println("Индексация остановлена пользователем");
            return createSuccessfulResponse();
        }
    }

    @Transactional
    public void indexSite(Site site) {
        try {
            siteCRUDService.deleteByUrl(site.getUrl());
            SiteDto siteDto = createSiteDto(site);
            siteCRUDService.create(siteDto);
            if (isSiteAccessible(site)) {
                ForkJoinPool pool = new ForkJoinPool();
                pool.invoke(new SiteMapper(site.getUrl(), siteDto, connectionProfile, pageCRUDService, siteCRUDService));
                pool.shutdown();
                if (!pool.awaitTermination(60, TimeUnit.MINUTES)) {
                    String errorMessage = "Error indexing site " + site.getUrl() + ": site is indexing for more than 1 hour.";
                    updateSiteStatus(site.getUrl(), Status.FAILED, errorMessage);
                } else {
                    updateSiteStatus(site.getUrl(), Status.INDEXED, null);
                }
            }
        } catch (InterruptedException e) {
            String errorMessage = "Индексация остановлена пользователем";
            updateSiteStatus(site.getUrl(), Status.FAILED, errorMessage);
        } catch (Exception e) {
            String errorMessage = "Error indexing site " + site.getUrl() + ": " + e.getMessage();
            updateSiteStatus(site.getUrl(), Status.FAILED, errorMessage);
        }
    }
    @Transactional
    public SiteDto createSiteDto (Site site) {
        SiteDto siteDto = new SiteDto();
        siteDto.setStatus(Status.INDEXING);
        siteDto.setStatusTime(Instant.now());
        siteDto.setLastError(null);
        siteDto.setUrl(site.getUrl());
        siteDto.setName(site.getName());
        return siteDto;
    }

    @Transactional
    public void updateSiteStatus(String siteUrl, Status status, String errorMessage) {
        SiteDto siteDto = siteCRUDService.getByUrl(siteUrl);
        if (siteDto != null) {
            siteDto.setStatus(status);
            siteDto.setStatusTime(Instant.now());
            siteDto.setLastError(errorMessage);
            siteCRUDService.update(siteDto);
        }
    }
    @Transactional
    public boolean isSiteAccessible(Site site) {
        try {
            Connection.Response response = Jsoup.connect(site.getUrl())
                    .userAgent(connectionProfile.getUserAgent())
                    .referrer(connectionProfile.getReferrer())
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute();
            int statusCode = response.statusCode();
            if (!(statusCode >= 200 && statusCode < 300)) {
                updateSiteStatus(site.getUrl(), Status.FAILED, "Site is not available. Error code: " + statusCode);
                return false;
            }
            return true;
        } catch (Exception e) {
            String errorMessage = "Error indexing site " + site.getUrl() + ": " + e.getMessage();
            updateSiteStatus(site.getUrl(), Status.FAILED, errorMessage);
            return false;
        }
    }

    private IndexingResponse createErrorResponse(String message) {
        IndexingResponse response = new IndexingResponse();
        response.setError(message);
        response.setResult(false);
        return response;
    }

    private IndexingResponse createSuccessfulResponse() {
        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }
}
