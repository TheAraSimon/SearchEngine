package searchengine.services.repositoryServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexDto;
import searchengine.model.Index;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexCRUDService {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public IndexDto createIndexDto(String lemma, Integer siteId, Integer pageId, Float rank) {
        try {
            IndexDto indexDto = new IndexDto();
            indexDto.setLemma(lemmaRepository.findLemmaByLemmaAndSiteId(lemma, siteId).get().getId());
            indexDto.setPage(pageRepository.findById(pageId).get().getId());
            indexDto.setRank(rank);
            return indexDto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Integer> getLemmaIdsByPageId(Integer pageId) {
        return indexRepository.findLemmaIdsByPageId(pageId);
    }

    public IndexDto getIndexByLemmaIdAndPageId(Integer lemmaId, Integer pageId) {
        Optional<Index> index = indexRepository.findIndexByLemmaIdAndPageId(lemmaId, pageId);
        if (index.isEmpty()) {
            log.warn("Index " + index + " was not found.");
            return null;
        } else {
            log.info("Get index: " + index);
            return mapToDto(index.get());
        }
    }

    public void create(IndexDto indexDto) {
        Index index = mapToModel(indexDto);
        index.setPage(pageRepository.findById(indexDto.getPage()).orElseThrow());
        index.setLemma(lemmaRepository.findById(indexDto.getLemma()).orElseThrow());
        index.setRank(index.getRank());
        indexRepository.save(index);
    }

    public void update(IndexDto indexDto) {
        if (!indexRepository.existsById(indexDto.getId())) {
            log.warn("Index with pageId"
                    .concat(indexDto.getPage().toString())
                    .concat(" and with lemmaId ")
                    .concat(indexDto.getLemma().toString())
                    .concat(" was not found."));
        } else {
            Index index = mapToModel(indexDto);
            index.setPage(pageRepository.findById(indexDto.getPage()).orElseThrow());
            index.setLemma(lemmaRepository.findById(indexDto.getLemma()).orElseThrow());
            index.setRank(index.getRank());
            indexRepository.save(index);
            log.info("Update index with pageId "
                    .concat(indexDto.getPage().toString())
                    .concat(" and with lemmaId ")
                    .concat(indexDto.getLemma().toString())
            );
        }
    }

    public static IndexDto mapToDto(Index index) {
        IndexDto indexDto = new IndexDto();
        indexDto.setId(index.getId());
        indexDto.setPage(index.getPage().getId());
        indexDto.setLemma(index.getLemma().getId());
        indexDto.setRank(index.getRank());
        return indexDto;
    }

    public static Index mapToModel(IndexDto indexDto) {
        Index index = new Index();
        index.setId(indexDto.getId());
        index.setRank(indexDto.getRank());
        return index;
    }
}
