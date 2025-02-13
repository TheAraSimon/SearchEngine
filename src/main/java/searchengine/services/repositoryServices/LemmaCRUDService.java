package searchengine.services.repositoryServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexDto;
import searchengine.dto.indexing.LemmaDto;
import searchengine.model.Lemma;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class LemmaCRUDService {
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final static Float PERCENT_OF_SITES_WITH_COMMON_LEMMAS = 0.8f;

    public LemmaDto getByLemmaAndSiteId(String lemmaToGet, Integer siteId) {
        Optional<Lemma> lemma = lemmaRepository.findLemmaByLemmaAndSiteId(lemmaToGet, siteId);
        return lemma.map(LemmaCRUDService::mapToDto).orElse(null);
    }

    public void create(LemmaDto lemmaDto) {
        Lemma lemma = mapToModel(lemmaDto);
        lemma.setFrequency(1);
        lemma.setSite(siteRepository.findById(lemmaDto.getSite()).orElseThrow());
        lemmaRepository.save(lemma);
    }

    public void update(LemmaDto lemmaDto) {
        if (!lemmaRepository.existsById(lemmaDto.getId())) {
//            log.warn("Lemma ".concat(lemmaDto.getLemma()).concat(" was not found."));
        } else {
            Lemma lemma = lemmaRepository.findById(lemmaDto.getId()).orElseThrow();
            Integer frequency = lemmaRepository.findLemmaByLemmaAndSiteId(lemmaDto.getLemma(), lemmaDto.getSite()).get().getFrequency();
            lemma.setFrequency(frequency + 1);
            lemmaRepository.save(lemma);
//            log.info("Update" + lemmaDto.getLemma());
        }
    }

    public LemmaDto createLemmaDto(String lemma, Integer siteId) {
        LemmaDto lemmaDto = new LemmaDto();
        lemmaDto.setSite(siteId);
        lemmaDto.setLemma(lemma);
        return lemmaDto;
    }

    public int getLemmaCountBySiteId(Integer siteId) {
        return lemmaRepository.countBySiteId(siteId);
    }

    public void deleteLemmasByIds(List<Integer> lemmaIds) {
        if (!lemmaIds.isEmpty()) {
            lemmaRepository.deleteByIds(lemmaIds);
        }
    }

    public static LemmaDto mapToDto(Lemma lemma) {
        LemmaDto lemmaDto = new LemmaDto();
        lemmaDto.setId(lemma.getId());
        lemmaDto.setSite(lemma.getSite().getId());
        lemmaDto.setLemma(lemma.getLemma());
        lemmaDto.setFrequency(lemma.getFrequency());
        return lemmaDto;
    }

    public static Lemma mapToModel(LemmaDto lemmaDto) {
        Lemma lemma = new Lemma();
        lemma.setId(lemmaDto.getId());
        lemma.setLemma(lemmaDto.getLemma());
//        lemma.setFrequency(lemma.getFrequency());
        return lemma;
    }

    public List<IndexDto> saveLemmasListAndCreateIndexes(Map<String, Integer> lemmas, int pageId, int siteId) {
        List<IndexDto> indexList = new CopyOnWriteArrayList<>();
        synchronized (lemmaRepository) {
            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                Optional<Lemma> lemma = lemmaRepository.findLemmaByLemmaAndSiteId(entry.getKey(), siteId);
                if (lemma.isEmpty()) {
                    Lemma lemmaToSave = new Lemma();
                    lemmaToSave.setSite(siteRepository.findById(siteId).orElseThrow());
                    lemmaToSave.setLemma(entry.getKey());
                    lemmaToSave.setFrequency(1);
                    lemmaRepository.save(lemmaToSave);
                } else {
                    Lemma lemmaToUpdate = lemma.get();
                    Integer frequency = lemmaToUpdate.getFrequency();
                    lemmaToUpdate.setFrequency(frequency + 1);
                    lemmaRepository.save(lemmaToUpdate);
                }
                Float rank = entry.getValue().floatValue();
                IndexDto indexDto = new IndexDto();
                indexDto.setPage(pageId);
                indexDto.setLemma(lemmaRepository.findLemmaByLemmaAndSiteId(entry.getKey(), siteId).get().getId());
                indexDto.setRank(rank);
                indexList.add(indexDto);
            }
        }
        return indexList;
    }

    public List<String> removeCommonLemmas(List<String> lemmas) {
        long totalSites = lemmaRepository.getTotalSiteCount();
        if (totalSites > 1) {
            long minSiteCount = (int) Math.ceil(totalSites * PERCENT_OF_SITES_WITH_COMMON_LEMMAS);
            List<String> commonLemmas = lemmaRepository.findCommonLemmas((int) minSiteCount);
            return lemmas.stream().filter(lemma -> !commonLemmas.contains(lemma)).toList();
        }
        return new ArrayList<>(lemmas);
    }

    public List<LemmaDto> getSortedLemmaDtos(List<String> lemmas, Integer siteId) {
        List<Lemma> sortedLemmas = lemmaRepository.findLemmasBySiteAndSort(lemmas, siteId);
        return sortedLemmas.stream().map(LemmaCRUDService::mapToDto).toList();
    }


}
