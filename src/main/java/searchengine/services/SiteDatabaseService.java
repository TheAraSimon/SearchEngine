package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SiteDatabaseService {

    private final SiteRepository siteRepository;

    @Transactional
    public void deleteSiteByUrl(Site site) {
        if (siteRepository.existsSiteByUrl(site.getUrl())) {
            siteRepository.deleteSiteByUrl(site.getUrl());
        }
    }

    @Transactional
    public void saveSite(Site site) {
        searchengine.model.Site siteToSaveInRepository = new searchengine.model.Site();
        siteToSaveInRepository.setStatus(Status.INDEXING);
        siteToSaveInRepository.setStatusTime(Instant.now());
        siteToSaveInRepository.setLastError(null);
        siteToSaveInRepository.setUrl(site.getUrl());
        siteToSaveInRepository.setName(site.getName());
        siteRepository.save(siteToSaveInRepository);
    }

    @Transactional
    public void updateSiteStatus(String siteUrl, Status status, String errorMessage) {
        searchengine.model.Site siteToUpdate = siteRepository.findSiteByUrl(siteUrl);
        if (siteToUpdate != null) {
            siteToUpdate.setStatus(status);
            siteToUpdate.setStatusTime(Instant.now());
            siteToUpdate.setLastError(errorMessage);
            siteRepository.save(siteToUpdate);
        }
    }
}
