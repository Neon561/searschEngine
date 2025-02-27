package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteStatus;
import searchengine.model.entity.Site;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site,Long> {

    Optional<Site>findByUrl(String url);

    @Modifying
    @Transactional
    @Query("UPDATE Site s SET s.siteStatus = :status, s.statusTime = :time WHERE s.id = :siteId")
    void updateStatus(@Param("siteId") long siteId, @Param("status") SiteStatus status, @Param("time") Instant time);


    @Modifying
    @Query("UPDATE Site s SET s.statusTime = CURRENT_TIMESTAMP WHERE s.id = :siteId")
    void updateStatusTime(Long siteId);

    @Modifying
    @Transactional
    @Query("UPDATE Site s SET s.lastError = 'индексация прервана пользователем', s.statusTime = CURRENT_TIMESTAMP")
    void indexingInterruptedByUser();

    @Query("SELECT COUNT(l) FROM Lemma l WHERE l.site.id = :siteId")
    int countLemmasBySiteId(@Param("siteId") long siteId);



}
