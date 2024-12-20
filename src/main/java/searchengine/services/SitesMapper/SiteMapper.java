package searchengine.services.SitesMapper;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionProfile;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
public class SiteMapper extends RecursiveTask<Void> {

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
            return null; // URL уже обрабатывался
        }

        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(connectionProfile.getUserAgent())
                    .referrer(connectionProfile.getReferrer())
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = response.statusCode();

            Document document = response.parse();
            if (isValidLink(url) && !pageRepository.existsPageByPath(url.substring(domain.length()))) {
                savePageInPageRepository(url, document, statusCode);
            }

            Elements links = document.select("a[href]");
            links.stream()
                    .map(link -> link.attr("abs:href"))
                    .filter(link -> isValidLink(link))
                    .forEach(link -> {
                        WebPageNode childNode = new WebPageNode(link);
                        currentNode.addChild(childNode);
                        SiteMapper task = new SiteMapper(link, childNode, domain, connectionProfile, pageRepository, siteRepository);
                        task.fork();
                    });

        } catch (Exception e) {
            logError("Ошибка при обработке URL: " + url + " - " + e.getMessage());
        }
        return null;
    }

    private void savePageInPageRepository(String link, Document document, int statusCode) {
        Site siteToUpdate = siteRepository.findSiteByUrl(domain);

        try {
            Page page = new Page();
            page.setSite(siteToUpdate);
            page.setPath(link.substring(domain.length()));
            page.setCode(statusCode);
            page.setContent(document.html());

            pageRepository.save(page);
            updateSiteStatusTime(siteToUpdate);
        }
        catch (Exception e) {
            logError("Ошибка сохранения страницы: " + link + " - " + e.getMessage());
        }
    }

    private void updateSiteStatusTime(Site site) {
        site.setStatusTime(Instant.now());
        siteRepository.save(site);
    }

    private boolean isValidLink(String link) {
        return link.startsWith(domain)
                && !link.contains("#")
                && !link.equals(domain);
    }

    private void logError(String message) {
        System.err.println(message);
    }
}
