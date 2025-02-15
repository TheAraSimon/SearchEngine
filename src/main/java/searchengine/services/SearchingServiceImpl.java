package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.LemmaDto;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.dto.searching.SearchResult;
import searchengine.dto.searching.SearchingResponse;
import searchengine.services.repositoryServices.IndexCRUDService;
import searchengine.services.repositoryServices.LemmaCRUDService;
import searchengine.services.repositoryServices.PageCRUDService;
import searchengine.services.repositoryServices.SiteCRUDService;

import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchingServiceImpl implements SearchingService {
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexCRUDService indexCRUDService;
    private final PageCRUDService pageCRUDService;
    private final SiteCRUDService siteCRUDService;
    private final SitesList sites;
    private final SearchingResponser searchingResponser = new SearchingResponser();
    private final List<SearchResult> data = new CopyOnWriteArrayList<>();

    @Override
    public List<String> getSiteUrlList() {
        return sites.getSites().stream().map(Site::getUrl).toList();
    }

    @Override
    public SearchingResponse search(String query, List<String> sitesList, int offset, int limit) {
        if (!data.isEmpty() && offset != 0 && data.size() > limit) {
            int start = Math.min(offset, data.size());
            int end = Math.min(start + limit, data.size());
            List<SearchResult> paginatedResults = data.subList(start, end);
            return searchingResponser.createSuccessfulResponse(data.size(), paginatedResults);
        }
        if (query.isEmpty()) {
            return searchingResponser.createErrorResponse("Задан пустой поисковый запрос");
        }
        if (indexCRUDService.isTableEmpty()) {
            return searchingResponser.createErrorResponse("Ни одна страница не была проиндексирована");
        }

        data.clear();
        Set<String> lemmas;
        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            lemmas = lemmaFinder.getLemmaSet(query);
        } catch (Exception e) {
            log.warn(e.getMessage());
            return searchingResponser.createErrorResponse("Задан некорректный поисковый запрос");
        }

        List<String> notCommonLemmas = lemmaCRUDService.removeCommonLemmas(new ArrayList<>(lemmas));
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String site : sitesList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    SiteDto siteDto = siteCRUDService.getByUrl(site);
                    if (siteDto == null) {
                        log.warn("SiteDto is null for site: " + site);
                        return;
                    }

                    List<LemmaDto> sortedLemmaDtos = lemmaCRUDService.getSortedLemmaDtos(notCommonLemmas, siteDto.getId());
                    List<PageDto> relevantPages = findRelevantPages(sortedLemmaDtos, siteDto);
                    calculateRelevance(relevantPages, sortedLemmaDtos, siteDto);
                } catch (Exception e) {
                    log.error("Error processing site: " + site, e);
                }
            }, executor);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        data.sort(Comparator.comparing(SearchResult::getRelevance).reversed());

        int start = Math.min(offset, data.size());
        int end = Math.min(start + limit, data.size());
        List<SearchResult> paginatedResults = data.subList(start, end);

        return searchingResponser.createSuccessfulResponse(data.size(), paginatedResults);
    }

    private List<PageDto> findRelevantPages(List<LemmaDto> sortedLemmaDtos, SiteDto siteDto) {
        if (sortedLemmaDtos.isEmpty()) {
            return Collections.emptyList();
        }
        List<LemmaDto> lemmasRelatedToSite = sortedLemmaDtos.stream()
                .filter(lemmaDto -> siteDto.getId().equals(lemmaDto.getSite()))
                .toList();
        if (lemmasRelatedToSite.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> relevantPageIds = new ArrayList<>(indexCRUDService.getPageIdsByLemmaId(lemmasRelatedToSite.get(0).getId()));

        for (int i = 1; i < lemmasRelatedToSite.size(); i++) {
            int lemmaId = lemmasRelatedToSite.get(i).getId();
            Set<Integer> pageIdsWithLemma = new HashSet<>(indexCRUDService.getPageIdsByLemmaId(lemmaId));
            relevantPageIds.retainAll(pageIdsWithLemma);
            if (relevantPageIds.isEmpty()) {
                return Collections.emptyList();
            }
        }

        return pageCRUDService.findPagesByIds(relevantPageIds);
    }

    private void calculateRelevance(List<PageDto> relevantPages, List<LemmaDto> lemmas, SiteDto siteDto) {
        float maxRelevance = 0f;
        String site = siteDto.getUrl();
        for (PageDto page : relevantPages) {
            float relevance = 0f;
            for (LemmaDto lemma : lemmas) {
                relevance += indexCRUDService.getIndexByLemmaIdAndPageId(lemma.getId(), page.getId()).getRank();
            }
            SearchResult searchResult = new SearchResult();
            searchResult.setSite(site.substring(0, site.length() - 1));
            searchResult.setSiteName(siteDto.getName());
            searchResult.setUri(page.getPath());
            searchResult.setTitle(getTitle(page));
            SnippetGenerator snippetGenerator = new SnippetGenerator(page, lemmas);
            searchResult.setSnippet(snippetGenerator.generateSnippet());
            searchResult.setRelevance(relevance);
            data.add(searchResult);
            maxRelevance = Math.max(maxRelevance, relevance);
        }

        for (SearchResult searchResult : data) {
            searchResult.setRelevance(searchResult.getRelevance() / maxRelevance);
        }
    }

    private String getTitle(PageDto pageDto) {
        String content = pageDto.getContent();
        Document document = Jsoup.parse(content);
        return document.title();
    }
}
