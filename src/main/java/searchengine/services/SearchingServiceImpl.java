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

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchingServiceImpl implements SearchingService {
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexCRUDService indexCRUDService;
    private final PageCRUDService pageCRUDService;
    private final SiteCRUDService siteCRUDService;
    private final SearchingResponser searchingResponser = new SearchingResponser();
    private final List<SearchData> data = new ArrayList<>();

    @Override
    public SearchingResponse search(String query, List<String> sitesList) {
        data.clear();
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

        List<String> notCommonLemmas = lemmaCRUDService.removeCommonLemmas(new ArrayList<>(lemmas));
        sitesList.forEach(site -> {
            SiteDto siteDto = siteCRUDService.getByUrl(site + "/");
            List<LemmaDto> sortedLemmaDtos = lemmaCRUDService.getSortedLemmaDtos(notCommonLemmas, siteDto.getId());
            List<PageDto> relevantPages = findRelevantPages(sortedLemmaDtos, siteDto);
            calculateRelevance(relevantPages, sortedLemmaDtos, siteDto);
        });
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
                relevance += indexCRUDService.getIndexByLemmaIdAndPageId(lemma.getId(), page.getId()).getRank();
            }
            SearchData searchData = new SearchData();
            searchData.setSite(site);
            searchData.setSiteName(siteDto.getName());
            searchData.setUri(page.getPath());
            searchData.setTitle(getTitle(page));
            //TODO
            searchData.setSnippet(null);
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
