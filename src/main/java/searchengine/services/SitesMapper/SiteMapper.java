package searchengine.services.SitesMapper;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionProfile;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
public class SiteMapper extends RecursiveTask<Void> {

    private static final Logger logger = LoggerFactory.getLogger(SiteMapper.class);
    private static final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    private final String url;
    private final WebPageNode currentNode;
    private final String domain;
    private final ConnectionProfile connectionProfile;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    @Override
    @Transactional
    public Void compute() {
        if (!visitedUrls.add(url)) {
            return null;
        }

        Site siteToUpdate = siteRepository.findSiteByUrl(domain);

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

            if (isValidLink(url) && !pageRepository.existsPageBySiteAndPath(siteToUpdate,url)) {
                savePageInPageRepository(url, document, statusCode, siteToUpdate);
            } else {
                logger.info("Страница уже существует в базе данных: {}", url);
            }

            Elements links = document.select("a[href]");
            links.stream()
                    .map(link -> link.attr("abs:href"))
                    .filter(this::isValidLink)
                    .forEach(link -> {
                        WebPageNode childNode = new WebPageNode(link);
                        currentNode.addChild(childNode);
                        SiteMapper task = new SiteMapper(link, childNode, domain, connectionProfile, pageRepository, siteRepository);
                        task.fork();
                    });

        } catch (UnsupportedMimeTypeException | SocketTimeoutException e) {
            System.out.print("");
//            logger.info("Ошибка (" + e.getMessage() + ") при обработке сайта {}", url);
        } catch (Exception e) {
            logger.error("Ошибка при обработке URL: {}", url + domain, e);
        }
        return null;
    }

    @Transactional
    public void savePageInPageRepository(String link, Document document, int statusCode, Site siteToUpdate) {
        Page page = new Page();
        page.setSite(siteToUpdate);
        page.setPath(link.substring(domain.length()));
        page.setCode(statusCode);
        page.setContent(document.html());
        try {
            pageRepository.save(page);
        } catch (Exception e) {
            logger.warn("Ошибка (" + e.getMessage() + ") при обработке сайта {}", link + domain);
        }
        siteToUpdate.setStatusTime(Instant.now());
        siteRepository.save(siteToUpdate);
    }


    private boolean isValidLink(String link) {
        return link.startsWith(domain)
                && !link.contains("#")
                && !link.equals(domain);
    }
}
