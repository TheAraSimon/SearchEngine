package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findByPathAndSiteId(String path, Integer siteId);

    int countBySiteId(Integer siteId);

    List<Page> findByIdIn(List<Integer> ids);
}
