package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionProfile;
import searchengine.dto.indexing.IndexDto;
import searchengine.dto.indexing.LemmaDto;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Index;
import searchengine.services.repositoryServices.IndexCRUDService;
import searchengine.services.repositoryServices.LemmaCRUDService;
import searchengine.services.repositoryServices.PageCRUDService;
import searchengine.services.repositoryServices.SiteCRUDService;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Slf4j
public class SiteMapper extends RecursiveTask<Void> {
    private static final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    private final String url;
    private final SiteDto siteDto;
    private final ConnectionProfile connectionProfile;
    private final PageCRUDService pageCRUDService;
    private final SiteCRUDService siteCRUDService;
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexCRUDService indexCRUDService;
    private static final AtomicBoolean stopRequested = new AtomicBoolean(false);

    public static void requestStop() {
        stopRequested.set(true);
    }

    public static void requestStart() {
        stopRequested.set(false);
    }

    public static void clearVisitedUrls() {
        visitedUrls.clear();
    }

    @Override
    public Void compute() {
        if (!visitedUrls.add(url)) {
            return null;
        }

        if (Thread.currentThread().isInterrupted() || stopRequested.get()) {
//            Turn on if required:
//            log.info("Задача прервана для URL: {}", url);
            return null;
        }

        try {
            UrlConnector urlConnector = new UrlConnector(url, connectionProfile);
            int statusCode = urlConnector.getStatusCode();
            if (statusCode >= 400) {
                return null;
            }

            Document document = urlConnector.getDocument();
            PageDto pageDto = pageCRUDService.createPageDto(url, document, statusCode, siteDto);
            try {
                pageCRUDService.create(pageDto);
                LemmaFinder lemmaFinder = LemmaFinder.getInstance();
                Map<String, Integer> lemmas = lemmaFinder.collectLemmas(urlConnector.getDocument().text());
                int pageId = pageCRUDService.getByUrlAndSiteId(pageDto.getPath(), pageDto.getSite()).getId();
                int siteId = siteCRUDService.getByUrl(siteDto.getUrl()).getId();
                List<IndexDto> indexList = new CopyOnWriteArrayList<>();
                synchronized (lemmaCRUDService) {
                    for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {

                        LemmaDto lemmaDto = lemmaCRUDService.getByLemmaAndSiteId(entry.getKey(), siteId);
                        if (lemmaDto == null) {
                            lemmaDto = lemmaCRUDService.createLemmaDto(entry.getKey(), siteId);
                            lemmaCRUDService.create(lemmaDto);
                        } else {
                            lemmaCRUDService.update(lemmaDto);
                        }

                        Float rank = entry.getValue().floatValue();
                        IndexDto indexDto = new IndexDto();
                        indexDto.setPage(pageId);
                        indexDto.setLemma(lemmaCRUDService.getByLemmaAndSiteId(entry.getKey(), siteId).getId());
                        indexDto.setRank(rank);
                        indexList.add(indexDto);
                    }
                }
                indexCRUDService.addAll(indexList);
            } catch (Exception e) {
                e.printStackTrace();
                log.warn("Ошибка (" + e.getMessage() + ") при обработке сайта {}", url);
            }


            Elements links = document.select("a[href]");
            links.stream()
                    .map(link -> link.attr("abs:href"))
                    .filter(this::isValidLink)
                    .forEach(link -> {
                        if (!stopRequested.get()) {
                            SiteMapper task = new SiteMapper(link, siteDto, connectionProfile,
                                    pageCRUDService, siteCRUDService, lemmaCRUDService, indexCRUDService);
                            task.fork();
                        }
                    });
            Thread.sleep(500 + (int) (Math.random() * 4500));
        } catch (UnsupportedMimeTypeException | SocketTimeoutException e) {
//            Turn on if required:
//            log.info("Ошибка (" + e.getMessage() + ") при обработке сайта {}", url);
        } catch (Exception e) {
            log.error("Ошибка при обработке URL: {}", url, e);
        }
        return null;
    }

    private boolean isValidLink(String link) {
        return link.startsWith(siteDto.getUrl())
                && !link.contains("#");
    }
}
