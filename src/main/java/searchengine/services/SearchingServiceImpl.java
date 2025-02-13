package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.LemmaDto;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.dto.searching.SearchData;
import searchengine.dto.searching.SearchingResponse;
import searchengine.services.repositoryServices.IndexCRUDService;
import searchengine.services.repositoryServices.LemmaCRUDService;
import searchengine.services.repositoryServices.PageCRUDService;
import searchengine.services.repositoryServices.SiteCRUDService;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchingServiceImpl implements SearchingService {
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexCRUDService indexCRUDService;
    private final PageCRUDService pageCRUDService;
    private final SiteCRUDService siteCRUDService;
    private final SearchingResponser searchingResponser = new SearchingResponser();
    private final List<SearchData> data = new CopyOnWriteArrayList<>();
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public SearchingResponse search(String query, List<String> sitesList) {

        if (query.isEmpty()) {
            return searchingResponser.createErrorResponse("Задан пустой поисковый запрос");
        }
        if (indexCRUDService.isTableEmpty()) {
            return searchingResponser.createErrorResponse("Ни одна страница не была проиндексирована");
        }

        Set<String> lemmas;
        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            lemmas = lemmaFinder.getLemmaSet(query);
        } catch (Exception e) {
            log.warn(e.getMessage());
            return searchingResponser.createErrorResponse("Задан некорректный поисковый запрос");
        }

        List<String> notCommonLemmas = lemmaCRUDService.removeCommonLemmas(lemmas.stream().toList());
        try {
            sitesList.forEach(site -> executorService.submit(() -> {
                SiteDto siteDto = siteCRUDService.getByUrl(site);
                List<LemmaDto> sortedLemmaDtos = lemmaCRUDService.getSortedLemmaDtos(notCommonLemmas, siteDto.getId());
                List<PageDto> relevantPages = findRelevantPages(sortedLemmaDtos, siteDto);
                calculateRelevance(relevantPages, sortedLemmaDtos, siteDto);
            }));
        } catch (Exception e) {
            log.warn(e.getMessage());
            return searchingResponser.createErrorResponse("Возникла ошибка во время поиска страниц");
        }
        data.sort(Comparator.comparing(SearchData::getRelevance).reversed());
        return searchingResponser.createSuccessfulResponse(data.size(), data);
    }

    public List<PageDto> findRelevantPages(List<LemmaDto> sortedLemmaDtos, SiteDto siteDto) {
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
                relevance += indexCRUDService.getIndexByLemmaIdAndPageId(page.getId(), lemma.getId()).getRank();
            }
            SearchData searchData = new SearchData();
            searchData.setSite(site);
            searchData.setSiteName(siteDto.getName());
            searchData.setUri(page.getPath());
            searchData.setTitle(getTitle(page));
            searchData.setSnippet("<li>Чарльз Петцольд «Код: тайный язык информатики»</li>\n" +
                    "<li>Егор Бугаенко «Элегантные Java объекты»</li>\n" +
                    "<li>Владстон Феррейра Фило «Теоретический минимум по Computer Science»</li>\n" +
                    "<li>Роберт Мартин «Чистая архитектура»</li>\n" +
                    "<li>Роберт Лафоре «Структуры данных и алгоритмы в Java»</li>\n" +
                    "<li>Филипе Гутьеррес «Spring Boot 2: лучшие практики для профессионалов»</li>\n" +
                    "<li>Алекс Сюй «System Design. Подготовка к сложному интервью»</li>");
//            searchData.setSite(site.substring(0, site.length() - 1));
//            searchData.setSiteName(siteDto.getName());
//            searchData.setUri(page.getPath().substring(0, page.getPath().length() - 1));
//            searchData.setTitle(getTitle(page));
//            TODO:
//            searchData.setSnippet(null);
            searchData.setRelevance(relevance);
            data.add(searchData);
            maxRelevance = Math.max(maxRelevance, relevance);
        }

        for (SearchData searchData : data) {
            searchData.setRelevance(searchData.getRelevance() / maxRelevance);
        }
    }

    private String getTitle(PageDto pageDto) {
        String content = pageDto.getContent();
        Document document = Jsoup.parse(content);
        return document.title();
    }

    @Override
    public List<String> getSiteUrlList() {
        return new ArrayList<String>();
    }
}
