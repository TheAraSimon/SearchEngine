package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findByPathAndSiteId(String path, Integer siteId);

    @Transactional(readOnly = true)
    @Query("SELECT COUNT(p) FROM Page p WHERE p.site.id = :siteId")
    int countBySiteId(@Param("siteId") Integer siteId);

    @Transactional(readOnly = true)
    @Query("SELECT p FROM Page p WHERE p.id IN :ids")
    List<Page> findPagesByIds(@Param("ids") List<Integer> ids);
}
