package searchengine.services.SitesMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionProfile;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.services.repositoryServices.PageCRUDService;
import searchengine.services.repositoryServices.SiteCRUDService;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
@Slf4j
public class SiteMapper extends RecursiveTask<Void> {
    private static final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    private final String url;
    private final WebPageNode currentNode;
    private final SiteDto siteDto;
    private final ConnectionProfile connectionProfile;
    private final PageCRUDService pageCRUDService;
    private final SiteCRUDService siteCRUDService;

    @Override
    @Transactional
    public Void compute() {
        if (!visitedUrls.add(url)) {
            return null;
        }

        try {
            Thread.sleep(500 + (int) (Math.random() * 4500));
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(connectionProfile.getUserAgent())
                    .referrer(connectionProfile.getReferrer())
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = response.statusCode();

            Document document = response.parse();

            if (isValidLink(url) && pageCRUDService.getByUrl(url.substring(siteDto.getUrl().length())) != null) {
                createPageDto(url, document, statusCode);
            } else {
                log.info("Страница уже существует в базе данных: {}", url);
            }

            Elements links = document.select("a[href]");
            links.stream()
                    .map(link -> link.attr("abs:href"))
                    .filter(this::isValidLink)
                    .forEach(link -> {
                        WebPageNode childNode = new WebPageNode(link);
                        currentNode.addChild(childNode);
                        SiteMapper task = new SiteMapper(link, childNode, siteDto, connectionProfile, pageCRUDService, siteCRUDService);
                        task.fork();
                    });

        } catch (UnsupportedMimeTypeException | SocketTimeoutException e) {
            log.info("Ошибка (" + e.getMessage() + ") при обработке сайта {}", url);
        } catch (Exception e) {
            log.error("Ошибка при обработке URL: {}", url, e);
        }
        return null;
    }

    @Transactional
    public void createPageDto (String link, Document document, int statusCode) {
        PageDto pageDto = new PageDto();
        pageDto.setSite(siteCRUDService.mapToModel(siteDto));
        pageDto.setPath(link.substring(siteDto.getUrl().length()));
        pageDto.setCode(statusCode);
        pageDto.setContent(document.html());
        try {
            pageCRUDService.create(pageDto);
        } catch (Exception e) {
            log.warn("Ошибка (" + e.getMessage() + ") при обработке сайта {}", link);
        }
        siteDto.setStatusTime(Instant.now());
        siteCRUDService.update(siteDto);
    }


    private boolean isValidLink(String link) {
        return link.startsWith(siteDto.getUrl())
                && !link.contains("#")
                && !link.equals(siteDto.getUrl());
    }
}
