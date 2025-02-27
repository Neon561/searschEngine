package searchengine.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseCleanupService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void clearAllData() {
        entityManager.createNativeQuery("DELETE FROM `index`").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM lemma").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM page").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM site").executeUpdate();

        System.out.println("Все данные удалены.");
    }
}
