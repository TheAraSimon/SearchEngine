package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    Optional<Index> findIndexByLemmaIdAndPageId(Integer lemmaId, Integer pageId);

    @Transactional(readOnly = true)
    @Query("SELECT i.lemma.id FROM Index i WHERE i.page.id = :pageId")
    List<Integer> findLemmaIdsByPageId(@Param("pageId") Integer pageId);

    @Transactional(readOnly = true)
    @Query("SELECT i.page.id FROM Index i WHERE i.lemma.id = :lemmaId")
    List<Integer> findPageIdsByLemmaId(@Param("lemmaId") Integer lemmaId);

    long count();
}
