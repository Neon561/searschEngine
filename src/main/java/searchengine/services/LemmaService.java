package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.model.entity.Page;
import searchengine.model.repository.IndexRepository;
import searchengine.model.repository.LemmaRepository;

import java.io.IOException;
import java.util.*;

@Service

public class LemmaService {
    private final LuceneMorphology russianMorphology;
    private final LuceneMorphology englishMorphology;
    private final LemmaRepository lemmaRepository;
    private final LemmaRepositoryService lemmaRepositoryService;

    private static final int BATCH_SIZE = 100;

    private static final Set<String> RUSSIAN_EXCLUDE_WORDS = Set.of("и","но", "в", "на", "с", "по", "к", "у", "о", "за", "не", "об", "обо");
    private static final Set<String> ENGLISH_EXCLUDE_WORDS = Set.of("and", "the", "in", "on", "with", "to", "of", "a", "an", "for");

    public static String[] extractWords(String text){

        return text.replaceAll("[^a-zA-Zа-яА-Я0-9\\s]", "").toLowerCase().split("\\s+");
    }

    public LemmaService(LemmaRepository lemmaRepository,
                        IndexRepository indexRepository,
                        LemmaRepositoryService lemmaRepositoryService) throws IOException {
        this.russianMorphology = new RussianLuceneMorphology();
        this.englishMorphology = new EnglishLuceneMorphology();
        this.lemmaRepository = lemmaRepository;
        this.lemmaRepositoryService = lemmaRepositoryService;
    }

    //
    public void searchAndSaveLemma(String text, Page page) {
        List<String> lemmas = searchLemma(text);
        Map<String, Integer> lemmaCountOnPage = new HashMap<>();

        for (String lemma : lemmas) {
            lemmaCountOnPage.merge(lemma, 1, Integer::sum);
        }

        lemmaRepositoryService.saveLemmasAndIndexes(lemmaCountOnPage, page);
    }

    public List<String> searchLemma(String text) {

        String[] words = extractWords(text);
        List<String> res = new ArrayList<>();
        for (String word : words) {
            if (!RUSSIAN_EXCLUDE_WORDS.contains(word) && !ENGLISH_EXCLUDE_WORDS.contains(word)) {
                String lemma = getLemma(word);
                res.add(lemma);
            }
        }
       return res;
    }

    private String getLemma(String word) {
        try {
            return russianMorphology.getNormalForms(word).get(0);
        } catch (Exception e) {
            try {
                return englishMorphology.getNormalForms(word).get(0);
            } catch (Exception ex) {
                return word.toLowerCase();
            }
        }

    }



}
