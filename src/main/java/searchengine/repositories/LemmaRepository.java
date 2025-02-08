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
}
