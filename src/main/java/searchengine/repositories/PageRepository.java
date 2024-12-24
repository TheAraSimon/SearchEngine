package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.nio.file.Path;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    boolean existsPageBySiteAndPath (Site site, String path);
}
