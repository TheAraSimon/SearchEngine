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

@Service
@RequiredArgsConstructor
public class SiteIndexingServiceImpl implements SiteIndexingService {
    private final SitesList sites;
    private final ConnectionProfile connectionProfile;
    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    @Transactional
    public IndexingResponse startIndexing() {
        List<Site> siteList = sites.getSites().stream().distinct().toList();
        if (siteList.isEmpty()) {
            IndexingResponse errorResponse = new IndexingResponse();
            errorResponse.setError("Sites list is empty");
            errorResponse.setResult(false);
            return errorResponse;
        }
        for (Site site : siteList) {
            executorService.submit(() -> indexSite(site));
        }
        executorService.shutdown();
        IndexingResponse successfulResponse = new IndexingResponse();
        successfulResponse.setResult(true);
        return successfulResponse;
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
}
