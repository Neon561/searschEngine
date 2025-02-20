package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.Index;
import searchengine.model.entity.Page;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    List<Index> findByPageId(Long pageId);

    @Query("SELECT p FROM Page p JOIN Index i ON p.id = i.page.id WHERE i.lemma.lemma = :lemmaText")
    List<Page> findPagesByLemmaText(@Param("lemmaText") String lemmaText);

    long countByLemmaId(int lemmaId);

    @Query("""
            SELECT p FROM Page p
            JOIN Index i ON p.id = i.page.id
            JOIN Lemma l ON i.lemma.id = l.id
            WHERE l.lemma IN :lemmas AND p.site.id = :siteId
            GROUP BY p.id
            HAVING COUNT(DISTINCT l.lemma) = :lemmasCount
            """)
    List<Page> findPagesByLemmas(@Param("lemmas") List<String> lemmas,
                                 @Param("siteId") Long siteId,
                                 @Param("lemmasCount") long lemmasCount);

    @Query("""
            SELECT p FROM Page p
            JOIN Index i ON p.id = i.page.id
            JOIN Lemma l ON i.lemma.id = l.id
            WHERE l.lemma IN :lemmas 
              AND p.id IN :pageIds
            GROUP BY p.id
            HAVING COUNT(DISTINCT l.lemma) = :lemmasCount
            """)
    List<Page> filterPagesByLemmasAndPageIds(@Param("lemmas") List<String> lemmas,
                                             @Param("pageIds") List<Long> pageIds,
                                             @Param("lemmasCount") long lemmasCount);

}
