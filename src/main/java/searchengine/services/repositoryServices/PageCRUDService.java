package searchengine.services.repositoryServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageCRUDService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    public PageDto getByUrlAndSiteId(String path, Integer site) {
        Optional<Page> page = pageRepository.findByPathAndSiteId(path, site);
        return page.map(this::mapToDto).orElse(null);
    }

    public void create(PageDto pageDto) {
        Site site = siteRepository.findById(pageDto.getSite()).orElseThrow();
        site.setStatusTime(Instant.now());
        siteRepository.save(site);
        Page page = mapToModel(pageDto);
        page.setSite(site);
        pageRepository.save(page);
    }

    public PageDto createPageDto(String link, Document document, int statusCode, SiteDto siteDto) {
        PageDto pageDto = new PageDto();
        pageDto.setSite(siteRepository.findSiteByUrl(siteDto.getUrl()).getId());
        pageDto.setPath(link.substring(siteDto.getUrl().length() - 1));
        pageDto.setCode(statusCode);
        pageDto.setContent(document.html());
        return pageDto;
    }

    public void deleteById(Integer id) {
        if (pageRepository.existsById(id)) {
            pageRepository.deleteById(id);
        }
    }

    public int getPageCountBySiteId(Integer siteId) {
        return pageRepository.countBySiteId(siteId);
    }

    public PageDto mapToDto(Page page) {
        PageDto pageDto = new PageDto();
        pageDto.setId(page.getId());
        pageDto.setSite(page.getSite().getId());
        pageDto.setPath(page.getPath());
        pageDto.setCode(page.getCode());
        pageDto.setContent(page.getContent());
        return pageDto;
    }

    public Page mapToModel(PageDto pageDto) {
        Page page = new Page();
        page.setId(pageDto.getId());
        page.setPath(pageDto.getPath());
        page.setCode(pageDto.getCode());
        page.setContent(pageDto.getContent());
        return page;
    }
}
