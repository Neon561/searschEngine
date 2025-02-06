package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.Index;
import searchengine.model.entity.Lemma;
import searchengine.model.entity.Page;

import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index,Long> {

}
