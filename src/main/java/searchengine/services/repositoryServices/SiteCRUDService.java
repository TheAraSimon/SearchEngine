package searchengine.services.repositoryServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
@Service
@RequiredArgsConstructor
@Slf4j
public class SiteCRUDService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

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
        log.info("Create");
    }

    public void update(SiteDto siteDto) {
        if (!siteRepository.existsSiteByUrl(siteDto.getUrl())) {
            log.warn("Site ".concat(siteDto.getUrl()).concat(" was not found."));
        } else {
            log.info("Update" + siteDto.getUrl());
            siteRepository.save(mapToModel(siteDto));
        }
    }

    public void deleteByUrl(String url) {
        if (siteRepository.existsSiteByUrl(url)) {
            siteRepository.deleteSiteByUrl(url);
        }
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
