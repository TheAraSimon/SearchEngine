package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Transactional
    @Query("DELETE FROM Lemma l WHERE l.id IN :ids")
    void deleteByIds(@Param("ids") List<Integer> ids);

    @Transactional(readOnly = true)
    @Query("SELECT COUNT(l) FROM Lemma l WHERE l.site.id = :siteId")
    int countBySiteId(@Param("siteId") Integer siteId);

    @Transactional(readOnly = true)
    @Query("SELECT COUNT(DISTINCT l.site.id) FROM Lemma l")
    long getTotalSiteCount();

    @Transactional(readOnly = true)
    @Query("SELECT l.lemma FROM Lemma l GROUP BY l.lemma HAVING COUNT(DISTINCT l.site.id) > :minSiteCount")
    List<String> findCommonLemmas(@Param("minSiteCount") long minSiteCount);

    @Transactional(readOnly = true)
    @Query("SELECT l FROM Lemma l WHERE l.lemma IN :lemmas AND l.site.id = :siteId ORDER BY l.frequency ASC")
    List<Lemma> findLemmasBySiteAndSort(@Param("lemmas") List<String> lemmas, @Param("siteId") Integer siteId);


}
