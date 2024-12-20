package searchengine.services.SitesMapper;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionProfile;
import searchengine.model.Page;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
public class SiteMapper extends RecursiveTask<Void> {

    private final String url;
    private final WebPageNode currentNode;
    private final String domain;
    private final ConnectionProfile connectionProfile;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    @Override
    @Transactional
    public Void compute() {

        if (pageRepository.existsPageByPath(url.substring(domain.length()))) {
            return null;
        }

        try {
            Document document = Jsoup.connect(url).userAgent(connectionProfile.getUserAgent())
                    .referrer(connectionProfile.getReferrer()).get();
            Elements links = document.select("a[href]");

            links.stream()
                    .map(link -> link.attr("abs:href"))
                    .filter(link -> link.startsWith(domain)
                            && !link.contains("#")
                            && !link.equals(domain))
                    .forEach(link -> {
                        WebPageNode childNode = new WebPageNode(link);
                        currentNode.addChild(childNode);
                        savePageInPageRepository(link);
                        SiteMapper task = new SiteMapper(link, childNode, domain, connectionProfile, pageRepository, siteRepository);
                        task.fork();
                        task.join();
                    });

            Thread.sleep(500 + (int) (Math.random() * 4500));

        } catch (UnsupportedMimeTypeException e) {
            System.out.println(e.getMessage());
            System.out.println("Ссылка ".concat(url.concat(" не является сайтом")));
            // прописать в логах
        } catch (Exception e) {
            System.out.println("Big problem");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Transactional
    public void savePageInPageRepository(String link) {
        searchengine.model.Site siteToUpdate = siteRepository.findSiteByUrl(domain);
        try {
            Page page = new Page();
            page.setSite(siteToUpdate);
            page.setPath(link.substring(domain.length()));
            Connection.Response response = Jsoup.connect(link)
                    .userAgent(connectionProfile.getUserAgent())
                    .referrer(connectionProfile.getReferrer())
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute();
            page.setCode(response.statusCode());
            Document document = response.parse();
            page.setContent(document.html());
            siteToUpdate.setStatusTime(Instant.now());
            pageRepository.save(page);
            siteRepository.save(siteToUpdate);
        } catch (UnsupportedMimeTypeException e) {
            System.out.println(link);
            System.out.println(e.getMessage());
            System.out.println("Ссылка ".concat(link).concat(" не является сайтом"));
            // прописать в логах
        } catch (DataIntegrityViolationException |
                 ConstraintViolationException e) {
            System.out.println(link);
            System.out.println(e.getMessage());
            System.out.println("DataIntegrityViolationException");
        } catch (SocketTimeoutException e) {
            System.out.println(link);
            System.out.println(e.getMessage());
            System.out.println("java.net.SocketTimeoutException: Read timed out");
        } catch (Exception e) {
            System.out.println(link);
            System.out.println(e.getMessage());
            e.printStackTrace();
            // прописать в логах
        }
    }
}
