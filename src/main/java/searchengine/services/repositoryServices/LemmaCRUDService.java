package searchengine.services.repositoryServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.LemmaDto;
import searchengine.model.Lemma;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LemmaCRUDService {
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    public LemmaDto getByLemmaAndSiteId(String lemmaToGet, Integer siteId) {
        Optional<Lemma> lemma = lemmaRepository.findLemmaByLemmaAndSiteId(lemmaToGet, siteId);
        if (lemma.isEmpty()) {
//            log.warn("Lemma " + lemma + " was not found.");
            return null;
        } else {
//            log.info("Get lemma: " + lemma);
            return mapToDto(lemma.get());
        }
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
}
