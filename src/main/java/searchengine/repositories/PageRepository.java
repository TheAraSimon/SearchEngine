package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    void deletePageByPath (String path);
    boolean existsPageByPath (String path);
    Optional <Page> findByPathAndSiteId(String path, Integer siteId);
}
