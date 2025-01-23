package searchengine.services.repositoryServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.PageDto;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageCRUDService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    public PageDto getByUrl(String path) {
        if (!pageRepository.existsPageByPath(path)) {
            return null;
        } else {
            log.info("Get page by path: " + path);
            Page page = pageRepository.findPageByPath(path);
            return mapToDto(page);
        }
    }

    public void create(PageDto pageDto) {
        Site site = siteRepository.findById(pageDto.getSite()).orElseThrow();
        site.setStatusTime(Instant.now());
        siteRepository.save(site);
        Page page = mapToModel(pageDto);
        page.setSite(site);
        pageRepository.save(page);
    }

    public void update(PageDto pageDto) {
        if (!pageRepository.existsPageByPath(pageDto.getPath())) {
            log.warn("Page ".concat(pageDto.getPath()).concat(" was not found."));
        } else {
            Site site = siteRepository.findById(pageDto.getSite()).orElseThrow();
            site.setStatusTime(Instant.now());
            Page page = mapToModel(pageDto);
            page.setSite(site);
            pageRepository.save(mapToModel(pageDto));
            log.info("Update" + pageDto.getPath());
        }
    }

    public void deleteByUrl(String path) {
        if (pageRepository.existsPageByPath(path)) {
            pageRepository.deletePageByPath(path);
        }
    }

    public static PageDto mapToDto(Page page) {
        PageDto pageDto = new PageDto();
        pageDto.setId(page.getId());
        pageDto.setSite(page.getSite().getId());
        pageDto.setPath(page.getPath());
        pageDto.setCode(page.getCode());
        pageDto.setContent(page.getContent());
        return pageDto;
    }

    public static Page mapToModel(PageDto pageDto) {
        Page page = new Page();
        page.setId(pageDto.getId());
        page.setPath(pageDto.getPath());
        page.setCode(pageDto.getCode());
        page.setContent(pageDto.getContent());
        return page;
    }
}
