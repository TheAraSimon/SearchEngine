package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionProfile;
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

    @Transactional
    public boolean isSiteAccessible (Site site, ConnectionProfile connectionProfile) {
        try {
            Connection.Response response = Jsoup.connect(site.getUrl())
                    .userAgent(connectionProfile.getUserAgent())
                    .referrer(connectionProfile.getReferrer())
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute();
            int statusCode = response.statusCode();
            if (!(statusCode >= 200 && statusCode < 300)) {
                updateSiteStatus(site.getUrl(),Status.FAILED, "Site is not available. Error code: " + statusCode);
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
