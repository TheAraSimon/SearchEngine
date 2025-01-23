package searchengine.services.repositoryServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteCRUDService implements CRUDService<SiteDto> {

    private final SiteRepository siteRepository;

    @Override
    public SiteDto getByUrl(String url) {
        if (!siteRepository.existsSiteByUrl(url)) {
            log.warn("Site " + url + " was not found.");
            return null;
        } else {
            log.info("Get site by url: " + url);
            Site site = siteRepository.findSiteByUrl(url);
            return mapToDto(site);
        }
    }

    @Override
    public void create(SiteDto siteDto) {
        log.info("Create");
        siteRepository.save(mapToModel(siteDto));
    }

    @Override
    public void update(SiteDto siteDto) {
        if (!siteRepository.existsSiteByUrl(siteDto.getUrl())) {
            log.warn("Site ".concat(siteDto.getUrl()).concat(" was not found."));
        } else {
            log.info("Update" + siteDto.getUrl());
            siteRepository.save(mapToModel(siteDto));
        }
    }

    @Override
    public void deleteByUrl(String url) {
        if (siteRepository.existsSiteByUrl(url)) {
            siteRepository.deleteSiteByUrl(url);
        }
    }

//    public void saveSite(Site site) {
//        searchengine.model.Site siteToSaveInRepository = new searchengine.model.Site();
//        siteToSaveInRepository.setStatus(Status.INDEXING);
//        siteToSaveInRepository.setStatusTime(Instant.now());
//        siteToSaveInRepository.setLastError(null);
//        siteToSaveInRepository.setUrl(site.getUrl());
//        siteToSaveInRepository.setName(site.getName());
//        siteRepository.save(siteToSaveInRepository);
//    }

//    public void updateSiteStatus(String siteUrl, Status status, String errorMessage) {
//        searchengine.model.Site siteToUpdate = siteRepository.findSiteByUrl(siteUrl);
//        if (siteToUpdate != null) {
//            siteToUpdate.setStatus(status);
//            siteToUpdate.setStatusTime(Instant.now());
//            siteToUpdate.setLastError(errorMessage);
//            siteRepository.save(siteToUpdate);
//        }
//    }
//
//    public boolean isSiteAccessible(Site site, ConnectionProfile connectionProfile) {
//        try {
//            Connection.Response response = Jsoup.connect(site.getUrl())
//                    .userAgent(connectionProfile.getUserAgent())
//                    .referrer(connectionProfile.getReferrer())
//                    .timeout(5000)
//                    .ignoreHttpErrors(true)
//                    .execute();
//            int statusCode = response.statusCode();
//            if (!(statusCode >= 200 && statusCode < 300)) {
//                updateSiteStatus(site.getUrl(), Status.FAILED, "Site is not available. Error code: " + statusCode);
//                return false;
//            }
//            return true;
//        } catch (Exception e) {
//            String errorMessage = "Error indexing site " + site.getUrl() + ": " + e.getMessage();
//            updateSiteStatus(site.getUrl(), Status.FAILED, errorMessage);
//            return false;
//        }
//    }

    public Site mapToModel(SiteDto siteDto) {
        Site site = new Site();
        site.setId(siteDto.getId());
        site.setStatus(siteDto.getStatus());
        site.setStatusTime(siteDto.getStatusTime());
        site.setLastError(siteDto.getLastError());
        site.setUrl(siteDto.getUrl());
        site.setName(site.getName());
        return site;
//
//        if (categoryDto.getNews() != null) {
//            category.setNews(categoryDto.getNews()
//                    .stream()
//                    .map(NewsCRUDService::mapToEntity)
//                    .toList()
//            );
//        }
    }

    public SiteDto mapToDto(Site site) {
        SiteDto siteDto = new SiteDto();
        siteDto.setId(site.getId());
        siteDto.setStatus(site.getStatus());
        siteDto.setStatusTime(site.getStatusTime());
        siteDto.setLastError(site.getLastError());
        siteDto.setUrl(site.getUrl());
        siteDto.setName(site.getName());
//        siteDto.setPage();
        return siteDto;
    }
}
