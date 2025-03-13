package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<Lemma> findLemmaByLemmaAndSiteId(String lemma, Integer siteId);

    @Transactional
    void deleteByIdIn(List<Integer> ids);

    int countBySiteId(Integer siteId);

    @Transactional(readOnly = true)
    @Query("SELECT COUNT(DISTINCT l.site.id) FROM Lemma l")
    long getTotalSiteCount();

    @Transactional(readOnly = true)
    @Query("SELECT l.lemma FROM Lemma l GROUP BY l.lemma HAVING COUNT(DISTINCT l.site.id) > :minSiteCount")
    List<String> findCommonLemmas(@Param("minSiteCount") long minSiteCount);

    List<Lemma> findByLemmaInAndSiteIdOrderByFrequencyAsc(List<String> lemmas, Integer siteId);


}
