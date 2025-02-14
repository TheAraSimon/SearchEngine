package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionProfile;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.*;
import searchengine.model.Status;
import searchengine.services.repositoryServices.IndexCRUDService;
import searchengine.services.repositoryServices.LemmaCRUDService;
import searchengine.services.repositoryServices.PageCRUDService;
import searchengine.services.repositoryServices.SiteCRUDService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexCRUDService indexCRUDService;
    private ExecutorService executorService;
    private final IndexingResponser indexingResponser = new IndexingResponser();
    private final AtomicBoolean isIndexing = new AtomicBoolean(false);

    @Override
    public IndexingResponse startIndexing() {
        if (isIndexing.get()) {
            return indexingResponser.createErrorResponse("Indexing is already in progress");
        }

        List<Site> sitesList = sites.getSites().stream().distinct().toList();
        if (sitesList.isEmpty()) {
            return indexingResponser.createErrorResponse("Site list is empty");
        }

        isIndexing.set(true);
        SiteMapper.requestStart();
        SiteMapper.clearVisitedUrls();

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        sitesList.forEach(site -> executorService.submit(() -> indexSite(site)));

        executorService.shutdown();
        log.info("Indexing started for {} sites", sitesList.size());
        return indexingResponser.createSuccessfulResponse();
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isIndexing.get()) {
            log.warn("Индексация не запущена");
            return indexingResponser.createErrorResponse("Индексация не запущена");
        }
        SiteMapper.requestStop();
        executorService.shutdownNow();
        isIndexing.set(false);
        log.info("Индексация остановлена пользователем");
        return indexingResponser.createSuccessfulResponse();
    }

    @Override
    public IndexingResponse indexPage(String url) {
        if (!isValidUrl(url)) {
            log.warn("Данная страница ({}) находится за пределами сайтов, указанных в конфигурационном файле", url);
            return indexingResponser.createErrorResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        Site site = getSiteFromUrl(url);
        if (!isSiteAccessible(site)) {
            return indexingResponser.createErrorResponse("Данная страница недоступна");
        }
        try {
            String pageUrl = url.substring(site.getUrl().length() - 1);
            int siteId = siteCRUDService.getByUrl(site.getUrl()).getId();
            PageDto pageDto = pageCRUDService.getByUrlAndSiteId(pageUrl, siteId);
            if (pageDto != null) {
                lemmaCRUDService.deleteLemmasByIds(indexCRUDService.getLemmaIdsByPageId(pageDto.getId()));
                pageCRUDService.deleteById(pageDto.getId());
                log.warn("Страница {} уже есть в базе данных", url);
            }
            UrlConnector urlConnector = new UrlConnector(url, connectionProfile);
            pageDto = pageCRUDService.createPageDto(url, urlConnector.getDocument(), urlConnector.getStatusCode(), siteCRUDService.getByUrl(site.getUrl()));
            pageCRUDService.create(pageDto);

            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            Map<String, Integer> lemmas = lemmaFinder.collectLemmas(urlConnector.getDocument().text());
            int pageId = pageCRUDService.getByUrlAndSiteId(pageDto.getPath(), pageDto.getSite()).getId();
            List<IndexDto> indexList = lemmaCRUDService.saveLemmasListAndCreateIndexes(lemmas, pageId, siteId);
            indexCRUDService.addAll(indexList);
            return indexingResponser.createSuccessfulResponse();
        } catch (Exception e) {
            return indexingResponser.createErrorResponse(e.getMessage());
        }
    }

    private void indexSite(Site site) {
        try {
            siteCRUDService.deleteByUrl(site.getUrl());
            SiteDto siteDto = siteCRUDService.createSiteDto(site);
            siteCRUDService.create(siteDto);
            if (isSiteAccessible(site)) {
                ForkJoinPool pool = new ForkJoinPool();
                pool.invoke(new SiteMapper(site.getUrl(), siteDto, connectionProfile,
                        pageCRUDService, siteCRUDService, lemmaCRUDService, indexCRUDService));
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

    private void updateSiteStatus(String siteUrl, Status status, String errorMessage) {
        SiteDto siteDto = siteCRUDService.getByUrl(siteUrl);
        if (siteDto != null) {
            siteDto.setStatus(status);
            siteDto.setStatusTime(Instant.now());
            siteDto.setLastError(errorMessage);
            siteCRUDService.update(siteDto);
        }
    }

    private boolean isSiteAccessible(Site site) {
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

    private Site getSiteFromUrl(String url) {
        List<Site> siteList = sites.getSites().stream().filter(site -> url.startsWith(site.getUrl())).distinct().toList();
        if (siteList.size() != 1) {
            log.error("Url " + url + " совпадает с несколькими сайтами из конфигурационного файла");
        }
        return siteList.get(0);
    }

    private boolean isValidUrl(String url) {
        return sites.getSites().stream().anyMatch(site -> url.startsWith(site.getUrl()));
    }
}