package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.LemmaDto;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.dto.searching.SearchResult;
import searchengine.dto.searching.SearchingResponse;
import searchengine.exceptions.searchingExceptions.EmptyIndexListException;
import searchengine.exceptions.searchingExceptions.EmptySearchQueryException;
import searchengine.exceptions.searchingExceptions.IncorrectSearchQueryException;
import searchengine.services.utilities.LemmaFinder;
import searchengine.services.api.SearchingService;
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
    private final List<SearchResult> data = new CopyOnWriteArrayList<>();

    @Override
    public List<String> getSiteUrlList() {
        return sites.getSites().stream().map(Site::getUrl).toList();
    }

    @Override
    public SearchingResponse search(String query, List<String> sitesList, int offset, int limit) {
        if (!data.isEmpty() && offset != 0 && data.size() > limit) {
            return getPaginatedResponse(offset, limit);
        }
        if (query.isEmpty()) {
            log.error("An empty search query was specified");
            throw new EmptySearchQueryException();
        }

        if (indexCRUDService.isTableEmpty()) {
            log.error("No pages were indexed");
            throw new EmptyIndexListException();
        }

        data.clear();
        List<String> notCommonLemmas = extractNotCommonLemmas(query);
        if (notCommonLemmas.isEmpty()) {
            log.error("An incorrect search query was specified");
            throw new IncorrectSearchQueryException();
        }

        processSitesParallel(sitesList, notCommonLemmas);

        data.sort(Comparator.comparing(SearchResult::getRelevance).reversed());
        return getPaginatedResponse(offset, limit);
    }

    private void processSitesParallel(List<String> sitesList, List<String> notCommonLemmas) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String site : sitesList) {
            futures.add(processSiteAsync(site, notCommonLemmas, executor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    }

    private CompletableFuture<Void> processSiteAsync(String site, List<String> notCommonLemmas, ExecutorService executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                SiteDto siteDto = siteCRUDService.getByUrl(site);
                if (siteDto == null) {
                    log.warn("SiteDto is null for site: {}", site);
                    return;
                }
                List<LemmaDto> sortedLemmaDtos = lemmaCRUDService.getSortedLemmaDtos(notCommonLemmas, siteDto.getId());
                List<PageDto> relevantPages = findRelevantPages(sortedLemmaDtos, siteDto);
                calculateRelevance(relevantPages, sortedLemmaDtos, siteDto);
            } catch (Exception e) {
                log.error("Error processing site: {}", site, e);
            }
        }, executor);
    }

    private List<String> extractNotCommonLemmas(String query) {
        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            Set<String> lemmas = lemmaFinder.getLemmaSet(query);
            return lemmaCRUDService.removeCommonLemmas(new ArrayList<>(lemmas));
        } catch (Exception e) {
            log.warn("Error extracting lemmas: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private SearchingResponse getPaginatedResponse(int offset, int limit) {
        int start = Math.min(offset, data.size());
        int end = Math.min(start + limit, data.size());
        List<SearchResult> paginatedResults = data.subList(start, end);
        return createSuccessfulResponse(data.size(), paginatedResults);
    }

    private List<PageDto> findRelevantPages(List<LemmaDto> sortedLemmaDtos, SiteDto siteDto) {
        if (sortedLemmaDtos.isEmpty()) {
            return Collections.emptyList();
        }
        List<LemmaDto> siteLemmas = sortedLemmaDtos.stream()
                .filter(lemmaDto -> siteDto.getId().equals(lemmaDto.getSite()))
                .toList();
        if (siteLemmas.isEmpty()) {
            return Collections.emptyList();
        }
        return findPagesByCommonLemmas(siteLemmas);
    }

    private List<PageDto> findPagesByCommonLemmas(List<LemmaDto> lemmas) {
        List<Integer> relevantPageIds = new ArrayList<>(indexCRUDService.getPageIdsByLemmaId(lemmas.get(0).getId()));

        for (int i = 1; i < lemmas.size(); i++) {
            Set<Integer> pageIds = new HashSet<>(indexCRUDService.getPageIdsByLemmaId(lemmas.get(i).getId()));
            relevantPageIds.retainAll(pageIds);
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

        if (maxRelevance > 0) {
            float finalMaxRelevance = maxRelevance;
            data.forEach(searchResult -> searchResult.setRelevance(searchResult.getRelevance() / finalMaxRelevance));
        }
    }

    private String getTitle(PageDto pageDto) {
        return Jsoup.parse(pageDto.getContent()).title();
    }

    private SearchingResponse createSuccessfulResponse(int count, List<SearchResult> data) {
        SearchingResponse response = new SearchingResponse();
        response.setCount(count);
        response.setData(data);
        response.setResult(true);
        return response;
    }
}
