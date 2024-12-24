package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionProfile;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SitesMapper.SiteMapper;
import searchengine.services.SitesMapper.WebPageNode;

import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class SiteIndexingServiceImpl implements SiteIndexingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sites;
    private final ConnectionProfile connectionProfile;
    private final SiteDatabaseService siteDatabaseService;

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
            siteDatabaseService.deleteSiteByUrl(site);
            siteDatabaseService.saveSite(site);

            ForkJoinPool pool = new ForkJoinPool();
            WebPageNode rootNode = new WebPageNode(site.getUrl());
            pool.invoke(new SiteMapper(site.getUrl(), rootNode, site.getUrl(), connectionProfile, pageRepository, siteRepository));
            pool.shutdown();
            pool.awaitTermination(60, TimeUnit.MINUTES);

            siteDatabaseService.updateSiteStatus(site.getUrl(), Status.INDEXED, null);
        } catch (Exception e) {
            String errorMessage = "Error indexing site " + site.getUrl() + ": " + e.getMessage();
            siteDatabaseService.updateSiteStatus(site.getUrl(), Status.FAILED, errorMessage);
        }
    }
}
