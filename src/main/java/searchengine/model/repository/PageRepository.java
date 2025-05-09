package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.Page;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    Optional<Page> findByPath(String path);
    int countBySiteId(Long siteId);

    boolean existsBySiteIdAndPath(Long siteId, String path);
}