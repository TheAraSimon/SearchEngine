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

    public List<Integer> getLemmaIdsByPageId(Integer pageId) {
        return indexRepository.findLemmaIdsByPageId(pageId);
    }
    public List<Integer> getPageIdsByLemmaId(Integer lemmaId) {
        return indexRepository.findPageIdsByLemmaId(lemmaId);
    }

    public IndexDto getIndexByLemmaIdAndPageId(Integer lemmaId, Integer pageId) {
        Optional<Index> index = indexRepository.findIndexByLemmaIdAndPageId(lemmaId, pageId);
        if (index.isEmpty()) {
            log.warn("Index " + index + " was not found.");
            return null;
        } else {
            return mapToDto(index.get());
        }
    }

    public IndexDto mapToDto(Index index) {
        IndexDto indexDto = new IndexDto();
        indexDto.setId(index.getId());
        indexDto.setPage(index.getPage().getId());
        indexDto.setLemma(index.getLemma().getId());
        indexDto.setRank(index.getRank());
        return indexDto;
    }

    public Index mapToModel(IndexDto indexDto) {
        Index index = new Index();
        index.setId(indexDto.getId());
        index.setRank(indexDto.getRank());
        return index;
    }

    public boolean isTableEmpty() {
        return indexRepository.count() == 0;
    }

    public void addAll(List<IndexDto> indexDtos) {
        if (indexDtos == null || indexDtos.isEmpty()) {
            log.warn("Передан пустой список для добавления в индекс.");
            return;
        }
        indexDtos.forEach((indexDto) -> {
            Index index = mapToModel(indexDto);
            index.setPage(pageRepository.findById(indexDto.getPage()).orElseThrow());
            index.setLemma(lemmaRepository.findById(indexDto.getLemma()).orElseThrow());
            index.setRank(index.getRank());
            indexRepository.save(index);
        });
    }
}
