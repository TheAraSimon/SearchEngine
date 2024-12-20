package searchengine.services.SitesMapper;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
public class SiteCrawler extends RecursiveTask<Set<String>> {
    private final String url;
    private final String domain;
    private static final Set<String> visitedUrls = new ConcurrentSkipListSet<>();

    @Override
    protected Set<String> compute() {
        Set<String> foundUrls = new ConcurrentSkipListSet<>();

        if (!visitedUrls.add(url)) {
            return foundUrls;
        }

        try {
            Document document = Jsoup.connect(url).get();
            Elements links = document.select("a[href]");

            List<SiteCrawler> subTasks = links.stream()
                    .map(link -> link.attr("abs:href"))
                    .filter(link -> link.startsWith(domain)
                            && !link.contains("#")
                            && !link.equals(domain))
                    .map(link -> new SiteCrawler(link, domain))
                    .toList();

            invokeAll(subTasks);

            for (SiteCrawler subTask : subTasks) {
                foundUrls.addAll(subTask.join());
            }

        } catch (Exception e) {
            System.err.println("Failed to visit: " + url + " due to " + e.getMessage());
        }

        foundUrls.add(url);
        return foundUrls;
    }
}