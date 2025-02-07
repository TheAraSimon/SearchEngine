package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
@Slf4j
public class SiteIndexingServiceImpl implements SiteIndexingService {

    private final SitesList sites;
    private final ConnectionProfile connectionProfile;
    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;
    private ExecutorService executorService;
    private final AtomicBoolean isIndexing = new AtomicBoolean(false);

    @Override
    public IndexingResponse startIndexing() {
        if (isIndexing.get()) {
            return createErrorResponse("Indexing is already in progress");
        }

        List<Site> siteList = sites.getSites().stream().distinct().toList();
        if (siteList.isEmpty()) {
            return createErrorResponse("Site list is empty");
        }

        isIndexing.set(true);
        SiteMapper.requestStart();
        SiteMapper.clearVisitedUrls();

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        siteList.forEach(site -> executorService.submit(() -> indexSite(site)));

        executorService.shutdown();
        log.info("Indexing started for {} sites", siteList.size());
        return createSuccessfulResponse();
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isIndexing.get()) {
            log.warn("Индексация не запущена");
            return createErrorResponse("Индексация не запущена");
        }
        SiteMapper.requestStop();
        executorService.shutdownNow();
        isIndexing.set(false);
        log.info("Индексация остановлена пользователем");
        return createSuccessfulResponse();
    }

    @Override
    public IndexingResponse indexPage(String url) {
        if (!isValidUrl(url)) {
            log.warn("Данная страница ({}) находится за пределами сайтов, указанных в конфигурационном файле", url);
            return createErrorResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        Site site = getSiteDtoFromUrl(url);
        if (!isSiteAccessible(site)) {
            return createErrorResponse("Данная страница недоступна");
        }
        try {
            if (pageCRUDService.getByUrlAndSiteId(url.substring(site.getUrl().length() - 1), siteCRUDService.getByUrl(site.getUrl()).getId()) != null) {
                log.warn("Страница {} уже есть в базе данных", url);
                return createErrorResponse("Страница " +  url + " уже есть в базе данных");
            }
            UrlConnector urlConnector = new UrlConnector(site.getUrl(), connectionProfile);
            SiteMapper siteMapper = new SiteMapper(url,
                    createSiteDto(site),
                    connectionProfile,
                    pageCRUDService,
                    siteCRUDService);
            siteMapper.createPageDto(url, urlConnector.getDocument(), urlConnector.getStatusCode());
            return createSuccessfulResponse();
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

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
                    String errorMessage = "Indexing timeout (more than 1 hour) for site: " + site.getUrl();
                    updateSiteStatus(site.getUrl(), Status.FAILED, errorMessage);
                } else if (pool.awaitQuiescence(60, TimeUnit.MINUTES)) {
                    updateSiteStatus(site.getUrl(), Status.INDEXED, null);
                }
            }
        } catch (InterruptedException e) {
            log.warn("Индексация остановлена для сайта: {}", site.getUrl());
            updateSiteStatus(site.getUrl(), Status.FAILED, "Индексация остановлена пользователем");
        } catch (Exception e) {
            log.error("Error indexing site {}: {}", site.getUrl(), e.getMessage());
            updateSiteStatus(site.getUrl(), Status.FAILED, "Error indexing site: " + e.getMessage());
        }
    }

    public SiteDto createSiteDto(Site site) {
        SiteDto siteDto = new SiteDto();
        siteDto.setStatus(Status.INDEXING);
        siteDto.setStatusTime(Instant.now());
        siteDto.setLastError(null);
        siteDto.setUrl(site.getUrl());
        siteDto.setName(site.getName());
        return siteDto;
    }

    public void updateSiteStatus(String siteUrl, Status status, String errorMessage) {
        SiteDto siteDto = siteCRUDService.getByUrl(siteUrl);
        if (siteDto != null) {
            siteDto.setStatus(status);
            siteDto.setStatusTime(Instant.now());
            siteDto.setLastError(errorMessage);
            siteCRUDService.update(siteDto);
        }
    }

    public boolean isSiteAccessible(Site site) {
        try {
            UrlConnector urlConnector = new UrlConnector(site.getUrl(), connectionProfile);
            int statusCode = urlConnector.getStatusCode();
            if (!(statusCode >= 200 && statusCode < 300)) {
                String errorMessage = "Site is not available. Error code: " + statusCode;
                updateSiteStatus(site.getUrl(), Status.FAILED, errorMessage);
                log.warn(errorMessage);
                return false;
            }
            return true;
        } catch (Exception e) {
            String errorMessage = "Error indexing site " + site.getUrl() + ": " + e.getMessage();
            updateSiteStatus(site.getUrl(), Status.FAILED, errorMessage);
            log.warn(errorMessage);
            return false;
        }
    }

    private Site getSiteDtoFromUrl(String url) {
        List<Site> siteList = sites.getSites().stream().filter(site -> url.startsWith(site.getUrl())).toList();
        if (siteList.size() != 1) {
            log.error("Url " + url + " совпадает с несколькими сайтами из конфигурационного файла");
        }
        return siteList.get(0);
    }

    private boolean isValidUrl(String url) {
        return sites.getSites().stream().anyMatch(site -> url.startsWith(site.getUrl()));
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