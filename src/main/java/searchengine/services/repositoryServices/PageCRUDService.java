package searchengine.services.repositoryServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.PageDto;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageCRUDService implements CRUDService<PageDto> {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Override
    public PageDto getByUrl(String path) {
        if (pageRepository.existsPageByPath(path)) {
            log.warn("Page " + path + " was not found.");
            return null;
        } else {
            log.info("Get page by path: " + path);
            Page page = pageRepository.findPageByPath(path);
            return mapToDto(page);
        }
    }

    @Override
    public void create(PageDto pageDto) {
        pageRepository.save(mapToModel(pageDto));
        Site site = siteRepository.findById(pageDto.getSite().getId()).orElseThrow();
        site.setStatusTime(Instant.now());
        siteRepository.save(site);
        log.info("Create page");
    }

    @Override
    public void update(PageDto pageDto) {
        if (!pageRepository.existsPageByPath(pageDto.getPath())) {
            log.warn("Page ".concat(pageDto.getPath()).concat(" was not found."));
        } else {
            log.info("Update" + pageDto.getPath());
            pageRepository.save(mapToModel(pageDto));
        }
    }

    @Override
    public void deleteByUrl(String path) {
        if (pageRepository.existsPageByPath(path)) {
            pageRepository.deletePageByPath(path);
        }
    }

    public static PageDto mapToDto(Page page) {
        PageDto pageDto = new PageDto();
        pageDto.setId(page.getId());
        pageDto.setSite(page.getSite());
        pageDto.setPath(page.getPath());
        pageDto.setCode(page.getCode());
        pageDto.setContent(page.getContent());
        return pageDto;
    }

    public static Page mapToModel(PageDto pageDto) {
        Page page = new Page();
        page.setId(pageDto.getId());
        page.setSite(pageDto.getSite());
        page.setPath(pageDto.getPath());
        page.setCode(pageDto.getCode());
        page.setContent(pageDto.getContent());
        return page;
    }
}
