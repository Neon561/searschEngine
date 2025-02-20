package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.SearchData;
import searchengine.model.entity.Index;
import searchengine.model.entity.Lemma;
import searchengine.model.entity.Page;
import searchengine.model.entity.Site;
import searchengine.model.repository.IndexRepository;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final LemmaService lemmaService;
    private final double TO_MORE_LEMMA = 0.9;

    //кол-во страниц для сайта получить из табл page
    //из lemma достать леммы которые встречаются на слишком большом количестве страниц относительно всех страниц сайта
    @Override
    @Transactional(readOnly = true)
    public List<SearchData> search(String query, String siteUrl, int offset, int limit) {
        // Разбиваем запрос на отдельные слова
        List<String> lemmas = lemmaService.searchLemma(query);

        // Получаем сайт по URL
        Site site = siteRepository.findByUrl(siteUrl)
                .orElseThrow(() -> new RuntimeException("Site not found"));

        int totalPages = pageRepository.countBySiteId(site.getId());
        // Рассчитываем порог
        int threshold = (int) (totalPages * TO_MORE_LEMMA);

        // Исключаем леммы, которые встречаются на слишком большом количестве страниц
        List<Lemma> filteredLemmas = new ArrayList<>();
        for (String lemma : lemmas) {
            Optional<Lemma> lemmaOptional = lemmaRepository.findByLemmaAndSite(lemma, site);
            if (lemmaOptional.isPresent()) {
                Lemma lemmaEntity = lemmaOptional.get();
                if (lemmaEntity.getFrequency() <= threshold) {
                    filteredLemmas.add(lemmaEntity);
                }
            }
        }

        // Если после фильтрации лемм не осталось, возвращаем пустой список
        if (filteredLemmas.isEmpty()) {
            return Collections.emptyList();
        }

        // Сортируем леммы по возрастанию частоты встречаемости
        filteredLemmas.sort(Comparator.comparingInt(Lemma::getFrequency));

        // Поиск страниц по самой редкой лемме
        Lemma rarestLemma = filteredLemmas.get(0);
        List<Page> pages = indexRepository.findPagesByLemmaText(rarestLemma.getLemma());

        // Если страниц не найдено, возвращаем пустой список
        if (pages.isEmpty()) {
            return Collections.emptyList();
        }

        // Получаем оставшиеся леммы
        List<String> remainingLemmas = filteredLemmas.stream()
                .map(Lemma::getLemma)
                .filter(lemma -> !lemma.equals(rarestLemma.getLemma()))
                .toList();

        // Фильтруем страницы по оставшимся леммам
        if (!remainingLemmas.isEmpty()) {
            pages = indexRepository.filterPagesByLemmasAndPageIds(
                    remainingLemmas,
                    pages.stream().map(Page::getId).toList(),
                    remainingLemmas.size()
            );

            // Если после фильтрации страниц не осталось, возвращаем пустой список
            if (pages.isEmpty()) {
                return Collections.emptyList();
            }
        }

        // Рассчитываем релевантность для каждой страницы
        Map<Page, Float> pageRankMap = new HashMap<>();
        for (Page page : pages) {
            float absoluteRelevance = 0;
            for (Lemma lemma : filteredLemmas) {
                // Находим индекс для текущей страницы и леммы
                Optional<Index> indexOptional = page.getIndices().stream()
                        .filter(index -> index.getLemma().equals(lemma))
                        .findFirst();
                if (indexOptional.isPresent()) {
                    absoluteRelevance += indexOptional.get().getRank();
                }
            }
            pageRankMap.put(page, absoluteRelevance);
        }

        // Находим максимальную абсолютную релевантность
        float maxRelevance = pageRankMap.values().stream()
                .max(Float::compareTo)
                .orElse(1f);

        // Сортируем страницы по убыванию релевантности
        List<Page> sortedPages = new ArrayList<>(pages);
        sortedPages.sort((p1, p2) -> Float.compare(pageRankMap.get(p2), pageRankMap.get(p1)));

        // Применяем пагинацию
        int start = offset;
        int end = Math.min(offset + limit, sortedPages.size());
        List<Page> paginatedPages = sortedPages.subList(start, end);

        // Формируем результат поиска
        List<SearchData> searchData = new ArrayList<>();
        for (Page page : paginatedPages) {
            SearchData result = new SearchData();
            result.setUri(page.getPath());
            result.setTitle(extractTitleFromContent(page.getContent()));
            result.setSnippet(extractSnippetFromContent(page.getContent(), query));
            result.setRelevance(pageRankMap.get(page) / maxRelevance); // Относительная релевантность
            result.setSite(siteUrl);
            result.setSiteName(site.getSiteName()); // Используем site из контекста
            searchData.add(result); // Добавляем результат в список
        }

        return searchData;
    }



    private String extractTitleFromContent(String htmlContent) {

        // Парсим HTML-код с помощью Jsoup
        Document document = Jsoup.parse(htmlContent);

        // Извлекаем заголовок страницы (тег <title>)
        String title = document.title();

        // Если заголовок пустой, пытаемся извлечь его из тега <h1>
        if (title == null || title.isEmpty()) {
            title = document.select("h1").text(); // Берем текст первого <h1>
        }

        // Если и <h1> пустой, возвращаем заголовок по умолчанию
        if (title == null || title.isEmpty()) {
            title = "Без заголовка";
        }

        return title;
    }
    private String extractSnippetFromContent(String htmlContent, String query) {
        if (htmlContent == null || htmlContent.isEmpty() || query == null || query.isEmpty()) {
            return ""; // Возвращаем пустую строку, если контент или запрос отсутствуют
        }

        // Парсим HTML-код с помощью Jsoup
        Document document = Jsoup.parse(htmlContent);

        // Извлекаем весь текст из HTML (без тегов)
        String plainText = document.text();

        // Разбиваем запрос на отдельные слова
        String[] queryWords = query.toLowerCase().split("\\s+");

        // Ищем позиции всех совпадений в тексте
        List<Integer> matchPositions = new ArrayList<>();
        for (String word : queryWords) {
            int wordPos = plainText.toLowerCase().indexOf(word);
            while (wordPos != -1) {
                matchPositions.add(wordPos);
                wordPos = plainText.toLowerCase().indexOf(word, wordPos + 1);
            }
        }

        // Если совпадений не найдено, возвращаем начало текста
        if (matchPositions.isEmpty()) {
            return plainText.length() > 200
                    ? plainText.substring(0, 200) + "..."
                    : plainText;
        }

        // Определяем границы сниппета
        int snippetStart = Math.max(0, matchPositions.get(0) - 100); // Начинаем сниппет за 100 символов до первого совпадения
        int snippetEnd = Math.min(plainText.length(), matchPositions.get(matchPositions.size() - 1) + 100); // Заканчиваем сниппет через 100 символов после последнего совпадения

        // Формируем сниппет
        String snippet = plainText.substring(snippetStart, snippetEnd);

        // Выделяем все совпадения тегом <b>
        for (String word : queryWords) {
            snippet = snippet.replaceAll("(?i)(" + word + ")", "<b>$1</b>");
        }

        // Добавляем многоточие, если сниппет обрезан
        if (snippetStart > 0) {
            snippet = "..." + snippet;
        }
        if (snippetEnd < plainText.length()) {
            snippet = snippet + "...";
        }

        return snippet;
    }
    public int getTotalCount(String query, String siteUrl) {
        Site site = siteRepository.findByUrl(siteUrl)
                .orElseThrow(() -> new RuntimeException("Site not found"));

        List<String> lemmas = lemmaService.searchLemma(query);
        int totalPages = pageRepository.countBySiteId(site.getId());
        int threshold = (int) (totalPages * TO_MORE_LEMMA);

        List<Lemma> filteredLemmas = lemmas.stream()
                .map(lemma -> lemmaRepository.findByLemmaAndSite(lemma, site))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(lemma -> lemma.getFrequency() <= threshold)
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .toList();

        if (filteredLemmas.isEmpty()) {
            return 0;
        }

        Lemma rarestLemma = filteredLemmas.get(0);
        List<Page> pages = indexRepository.findPagesByLemmaText(rarestLemma.getLemma());

        if (pages.isEmpty()) {
            return 0;
        }

        List<String> remainingLemmas = filteredLemmas.stream()
                .map(Lemma::getLemma)
                .filter(lemma -> !lemma.equals(rarestLemma.getLemma()))
                .toList();

        if (!remainingLemmas.isEmpty()) {
            pages = indexRepository.filterPagesByLemmasAndPageIds(
                    remainingLemmas,
                    pages.stream().map(Page::getId).toList(),
                    remainingLemmas.size()
            );
        }

        return pages.size();
    }

    //todo завернуть в for each  для кажгого siteUrl или вообще убрать сайт и просто считать колличество страниц
    public int getTotalCount(String query) {
        int res  = 0;
        List<Site> sites = siteRepository.findAll();
        for (Site siteUrl : sites) {
            Site site = siteRepository.findByUrl(siteUrl.getUrl())
                    .orElseThrow(() -> new RuntimeException("Site not found"));

            List<String> lemmas = lemmaService.searchLemma(query);
            int totalPages = pageRepository.countBySiteId(site.getId());
            int threshold = (int) (totalPages * TO_MORE_LEMMA);

            List<Lemma> filteredLemmas = lemmas.stream()
                    .map(lemma -> lemmaRepository.findByLemmaAndSite(lemma, site))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(lemma -> lemma.getFrequency() <= threshold)
                    .sorted(Comparator.comparingInt(Lemma::getFrequency))
                    .toList();

            if (filteredLemmas.isEmpty()) {
                return 0;
            }

            Lemma rarestLemma = filteredLemmas.get(0);
            List<Page> pages = indexRepository.findPagesByLemmaText(rarestLemma.getLemma());

            if (pages.isEmpty()) {
                return 0;
            }

            List<String> remainingLemmas = filteredLemmas.stream()
                    .map(Lemma::getLemma)
                    .filter(lemma -> !lemma.equals(rarestLemma.getLemma()))
                    .toList();

            if (!remainingLemmas.isEmpty()) {
                pages = indexRepository.filterPagesByLemmasAndPageIds(
                        remainingLemmas,
                        pages.stream().map(Page::getId).toList(),
                        remainingLemmas.size()
                );
            }
            res += pages.size();

        }
        return res;
    }
    public List<SearchData> searchAllSites(String query, int offset, int limit) {
        List<SearchData> allResults = new ArrayList<>();

        // Получаем все сайты из таблицы Site
        List<Site> sites = siteRepository.findAll();

        // Для каждого сайта вызываем метод search
        for (Site site : sites) {
            List<SearchData> siteResults = search(query, site.getUrl(), offset, limit);
            allResults.addAll(siteResults);
        }

        // Сортируем результаты по релевантности (если нужно)
        allResults.sort((r1, r2) -> Float.compare(r2.getRelevance(), r1.getRelevance()));

        // Применяем пагинацию
        int start = offset;
        int end = Math.min(offset + limit, allResults.size());
        return allResults.subList(start, end);
    }
}

