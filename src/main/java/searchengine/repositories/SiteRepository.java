package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    void deleteSiteByUrl(String url);

    Site findSiteByUrl(String url);

    boolean existsSiteByUrl(String url);

    long count();

    List<Site> findAll();
}
