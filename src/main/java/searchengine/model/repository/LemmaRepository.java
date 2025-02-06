package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.Lemma;
import searchengine.model.entity.Site;

import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);
}
