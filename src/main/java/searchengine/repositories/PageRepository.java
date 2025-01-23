package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.nio.file.Path;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    void deletePageByPath (String path);
    boolean existsPageByPath (String path);
    Page findPageByPath (String path);
}
