package searchengine.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entity.Lemma;
import searchengine.model.repository.LemmaRepository;
import java.util.*;

@Service
public class LemmaRepositoryService {
    private final LemmaRepository lemmaRepository;

    public LemmaRepositoryService(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    @Transactional
    public void saveLemmas(Map<String, Map<Long, Integer>> lemmaData) {
        List<Lemma> batch = new ArrayList<>();

        for (Map.Entry<String, Map<Long, Integer>> entry : lemmaData.entrySet()) {
            String lemma = entry.getKey();
            for (Map.Entry<Long, Integer> siteEntry : entry.getValue().entrySet()) {
                Long siteId = siteEntry.getKey();
                int frequency = siteEntry.getValue();

                Lemma lemmaEntity = lemmaRepository.findByLemmaAndSite(lemma, siteId)
                        .orElseGet(() -> {
                            Lemma newLemma = new Lemma();
                            newLemma.setLemma(lemma);
                            newLemma.setSite(site);
                            newLemma.setFrequency(0);
                            return newLemma;
                        });

                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + frequency);
                batch.add(lemmaEntity);
            }
        }

        lemmaRepository.saveAll(batch); // Используем batch insert
    }
}
