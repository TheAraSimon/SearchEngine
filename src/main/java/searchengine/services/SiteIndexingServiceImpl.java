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

import java.time.Instant;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class SiteIndexingServiceImpl implements SiteIndexingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sites;
    private final ConnectionProfile connectionProfile;

    @Override
    @Transactional
    public IndexingResponse startIndexing() {
        if (sites.getSites().isEmpty()) {
            IndexingResponse errorResponse = new IndexingResponse();
            errorResponse.setError("sites list is empty");
            errorResponse.setResult(false);
            return errorResponse;
        } else {

            sites.getSites().forEach(site -> {
                deleteSiteFromSiteRepository(site);
                saveSiteInSiteRepository(site);
                ForkJoinPool pool = new ForkJoinPool();
                WebPageNode rootNode = new WebPageNode(site.getUrl());
                pool.invoke(new SiteMapper(site.getUrl(), rootNode, site.getUrl(), connectionProfile, pageRepository, siteRepository));
                pool.shutdown();
                searchengine.model.Site siteToSaveInRepository = siteRepository.findSiteByUrl(site.getUrl());
                siteToSaveInRepository.setStatusTime(Instant.now());
                siteToSaveInRepository.setStatus(Status.INDEXED);
                siteRepository.save(siteToSaveInRepository);
            });

            IndexingResponse successfulResponse = new IndexingResponse();
            successfulResponse.setResult(true);
            return successfulResponse;
        }
    }

    @Transactional
    public void saveSiteInSiteRepository(Site site) {
        searchengine.model.Site siteToSaveInRepository = new searchengine.model.Site();
        siteToSaveInRepository.setStatus(Status.INDEXING);
        siteToSaveInRepository.setStatusTime(Instant.now());
        siteToSaveInRepository.setLastError(null);
        siteToSaveInRepository.setUrl(site.getUrl());
        siteToSaveInRepository.setName(site.getName());
        siteRepository.save(siteToSaveInRepository);
    }

    @Transactional
    public void deleteSiteFromSiteRepository(Site site) {
        if (siteRepository.existsSiteByUrl(site.getUrl())) {
            siteRepository.deleteSiteByUrl(site.getUrl());
        }
    }
}
