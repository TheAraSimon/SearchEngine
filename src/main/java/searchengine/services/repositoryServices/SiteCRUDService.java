package searchengine.services.repositoryServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteCRUDService {

    private final SiteRepository siteRepository;

    public SiteDto getByUrl(String url) {
        if (!siteRepository.existsSiteByUrl(url)) {
            log.warn("Site " + url + " was not found.");
            return null;
        } else {
            Site site = siteRepository.findSiteByUrl(url);
            return mapToDto(site);
        }
    }

    public void create(SiteDto siteDto) {
        Site site = mapToModel(siteDto);
        siteRepository.save(site);
        log.info("Create" + siteDto.getUrl());
    }

    public void update(SiteDto siteDto) {
        if (!siteRepository.existsSiteByUrl(siteDto.getUrl())) {
            log.warn("Site ".concat(siteDto.getUrl()).concat(" was not found."));
        } else {
            log.info("Update" + siteDto.getUrl());
            siteRepository.save(mapToModel(siteDto));
        }
    }

    @Transactional
    public void deleteByUrl(String url) {
        if (siteRepository.existsSiteByUrl(url)) {
            siteRepository.deleteSiteByUrl(url);
        }
    }

    public int countNumberOfSitesInDB() {
        return (int) siteRepository.count();
    }

    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }

    public SiteDto createSiteDto(searchengine.config.Site site) {
        SiteDto siteDto = new SiteDto();
        siteDto.setStatus(Status.INDEXING);
        siteDto.setStatusTime(Instant.now());
        siteDto.setLastError(null);
        siteDto.setUrl(site.getUrl());
        siteDto.setName(site.getName());
        return siteDto;
    }

    public Site mapToModel(SiteDto siteDto) {
        Site site = new Site();
        site.setId(siteDto.getId());
        site.setStatus(siteDto.getStatus());
        site.setStatusTime(siteDto.getStatusTime());
        site.setLastError(siteDto.getLastError());
        site.setUrl(siteDto.getUrl());
        site.setName(siteDto.getName());
        return site;
    }

    public SiteDto mapToDto(Site site) {
        SiteDto siteDto = new SiteDto();
        siteDto.setId(site.getId());
        siteDto.setStatus(site.getStatus());
        siteDto.setStatusTime(site.getStatusTime());
        siteDto.setLastError(site.getLastError());
        siteDto.setUrl(site.getUrl());
        siteDto.setName(site.getName());
        return siteDto;
    }
}
