package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.model.repository.IndexRepository;
import searchengine.model.repository.LemmaRepository;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service

public class LemmaService {
    private final LuceneMorphology russianMorphology;
    private final LuceneMorphology englishMorphology;
    private final LemmaRepository lemmaRepository;
    private final ConcurrentHashMap<String, Map<Long, Integer>> lemmaFrequencyMap;
    private static final int BATCH_SIZE = 2000;

    private static final Set<String> RUSSIAN_STOP_WORDS = Set.of("и", "в", "на", "с", "по", "к", "у", "о", "за", "не", "об", "обо");
    private static final Set<String> ENGLISH_STOP_WORDS = Set.of("and", "the", "in", "on", "with", "to", "of", "a", "an", "for");



    public LemmaService(LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException {
        this.russianMorphology = new RussianLuceneMorphology();
        this.englishMorphology = new EnglishLuceneMorphology();
        this.lemmaRepository = lemmaRepository;
        lemmaFrequencyMap = new ConcurrentHashMap<String, Map<Long, Integer>>();


    }
//todo добавить уникальный индекс на лемму + site id чтобы фильтровать дубликаты на уровне базы.
    public void searchLemma(String text,Long siteId) {
        String[] words = text.replaceAll("[^a-zA-Zа-яА-Я0-9\\s]", "").toLowerCase().split("\\s+");

        for (String word : words) {
            if (!RUSSIAN_STOP_WORDS.contains(word) || ENGLISH_STOP_WORDS.contains(word)) {
                String lemma = getLemma(word);


                lemmaFrequencyMap
                        .computeIfAbsent(lemma, k -> new ConcurrentHashMap<>()) // Создаём вложенную Map, если леммы ещё нет
                        .merge(siteId, 1, Integer::sum); // Увеличиваем частоту леммы для данного siteId


            }
            if (lemmaFrequencyMap.size()>=BATCH_SIZE){
                saveLemmas(lemmaFrequencyMap);
            }

        }

    }

    private String getLemma(String word) {
        try {
            return russianMorphology.getNormalForms(word).get(0); // Пытаемся проанализировать как русское слово
        } catch (Exception e) {
            try {
                return englishMorphology.getNormalForms(word).get(0); // Пытаемся проанализировать как английское слово
            } catch (Exception ex) {
                return word.toLowerCase(); // Если не удалось, возвращаем слово как есть
            }
        }
    }
    private void saveLemmas(Map<String, Map<Long, Integer>> lemmaData){


        //todo
    }

}
