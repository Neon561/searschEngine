package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site,Long> {

    void deleteByUrl(String url);
   // List<Site> findAll();

    Optional<Site>findByUrl(String url);

    @Modifying
    @Query("UPDATE Site s SET s.statusTime = CURRENT_TIMESTAMP WHERE s.id = :siteId")
    void updateStatusTime(Long siteId);
}
