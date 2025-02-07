package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    Optional<Index> findIndexByLemmaIdAndPageId (Integer lemmaId, Integer pageId);
}
