package searchengine.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entity.Index;
import searchengine.model.entity.Lemma;
import searchengine.model.entity.Page;
import searchengine.model.entity.Site;
import searchengine.model.repository.IndexRepository;
import searchengine.model.repository.LemmaRepository;
import java.util.*;

@Service
public class LemmaRepositoryService {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public LemmaRepositoryService(LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }
    private static final int BATCH_SIZE = 300;


    @Transactional
    public void saveLemmasAndIndexes(Map<String, Integer> lemmaCountOnPage, Page page) {
        Site site = page.getSite();
        List<Lemma> lemmasToSave = new ArrayList<>();
        List<Index> indexesToSave = new ArrayList<>();
        Map<String, Lemma> existingLemmas = new HashMap<>();

        for (String lemmaText : lemmaCountOnPage.keySet()) {
            lemmaRepository.findByLemmaAndSite(lemmaText, site)
                    .ifPresent(lemma -> existingLemmas.put(lemmaText, lemma));
        }

        for (Map.Entry<String, Integer> entry : lemmaCountOnPage.entrySet()) {
            String lemmaText = entry.getKey();
            int lemmaCount = entry.getValue();

            Lemma lemma = existingLemmas.computeIfAbsent(lemmaText, k -> {
                Lemma newLemma = new Lemma();
                newLemma.setLemma(k);
                newLemma.setSite(site);
                newLemma.setFrequency(0);
                lemmasToSave.add(newLemma);
                return newLemma;
            });

            lemma.setFrequency(lemma.getFrequency() + 1);

            Index index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(lemmaCount);
            indexesToSave.add(index);

            if (lemmasToSave.size() >= BATCH_SIZE) {
                batchSave(lemmasToSave, indexesToSave);
            }
        }

        batchSave(lemmasToSave, indexesToSave);
    }

    private void batchSave(List<Lemma> lemmasToSave, List<Index> indexesToSave) {
        if (!lemmasToSave.isEmpty()) {
            lemmaRepository.saveAll(lemmasToSave);
            lemmasToSave.clear();
        }
        if (!indexesToSave.isEmpty()) {
            indexRepository.saveAll(indexesToSave);
            indexesToSave.clear();
        }

    }
}
