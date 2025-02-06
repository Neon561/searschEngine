package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.Site;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site,Long> {

    void deleteByUrl(String url);

    Optional<Site>findByUrl(String url);
}
