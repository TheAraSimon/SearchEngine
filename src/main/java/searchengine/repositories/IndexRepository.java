package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    Optional<Index> findIndexByLemmaIdAndPageId(Integer lemmaId, Integer pageId);

    @Query("SELECT i.lemma.id FROM Index i WHERE i.page.id = :pageId")
    List<Integer> findLemmaIdsByPageId(@Param("pageId") Integer pageId);
}
