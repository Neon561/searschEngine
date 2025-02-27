package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

    @Override
    @Transactional(readOnly = true)
    public List<SearchData> search(String query, String siteUrl, int offset, int limit) {
        List<String> lemmas = lemmaService.searchLemma(query);

        Site site = siteRepository.findByUrl(siteUrl)
                .orElseThrow(() -> new RuntimeException("Site not found"));

        int totalPages = pageRepository.countBySiteId(site.getId());
        int threshold = (int) (totalPages * TO_MORE_LEMMA);

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

        if (filteredLemmas.isEmpty()) {
            return Collections.emptyList();
        }

        filteredLemmas.sort(Comparator.comparingInt(Lemma::getFrequency));

        Lemma rarestLemma = filteredLemmas.get(0);
        List<Page> pages = indexRepository.findPagesByLemmaText(rarestLemma.getLemma());

        if (pages.isEmpty()) {
            return Collections.emptyList();
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

            if (pages.isEmpty()) {
                return Collections.emptyList();
            }
        }

        Map<Page, Float> pageRankMap = new HashMap<>();
        for (Page page : pages) {
            float absoluteRelevance = 0;
            for (Lemma lemma : filteredLemmas) {
                Optional<Index> indexOptional = page.getIndices().stream()
                        .filter(index -> index.getLemma().equals(lemma))
                        .findFirst();
                if (indexOptional.isPresent()) {
                    absoluteRelevance += indexOptional.get().getRank();
                }
            }
            pageRankMap.put(page, absoluteRelevance);
        }

        float maxRelevance = pageRankMap.values().stream()
                .max(Float::compareTo)
                .orElse(1f);

        List<Page> sortedPages = new ArrayList<>(pages);
        sortedPages.sort((p1, p2) -> Float.compare(pageRankMap.get(p2), pageRankMap.get(p1)));

        int start = offset;
        int end = Math.min(offset + limit, sortedPages.size());
        List<Page> paginatedPages = sortedPages.subList(start, end);


        List<SearchData> searchData = new ArrayList<>();
        for (Page page : paginatedPages) {
            SearchData result = new SearchData();
            result.setUri(page.getPath());
            result.setTitle(extractTitleFromContent(page.getContent()));
            result.setSnippet(extractSnippetFromContent(page.getContent(), query));
            result.setRelevance(pageRankMap.get(page) / maxRelevance);
            result.setSite(siteUrl);
            result.setSiteName(site.getSiteName());
            searchData.add(result);
        }

        return searchData;
    }


    private String extractTitleFromContent(String htmlContent) {

        Document document = Jsoup.parse(htmlContent);
        String title = document.title();

        if (title == null || title.isEmpty()) {
            title = document.select("h1").text();
        }

        if (title == null || title.isEmpty()) {
            title = "Без заголовка";
        }

        return title;
    }

    private String extractSnippetFromContent(String htmlContent, String query) {
        if (htmlContent == null || htmlContent.isEmpty() || query == null || query.isEmpty()) {
            return "";
        }
        Document document = Jsoup.parse(htmlContent);
        String plainText = document.text();

        String[] words = plainText.split("\\s+");

        String[] queryWords = query.toLowerCase().split("\\s+");

        List<String> snippets = new ArrayList<>();

        Set<Integer> matchIndexes = new TreeSet<>();
        for (String word : queryWords) {
            for (int i = 0; i < words.length; i++) {
                if (words[i].equalsIgnoreCase(word)) {
                    matchIndexes.add(i);
                }
            }
        }

        if (matchIndexes.isEmpty()) {
            return plainText.length() > 200 ? plainText.substring(0, 200) + "..." : plainText;
        }

        int snippetSize = 10;
        int maxSnippets = 3;
        int lastEnd = -1;

        for (int index : matchIndexes) {
            int start = Math.max(0, index - snippetSize);
            int end = Math.min(words.length, index + snippetSize);

            if (start <= lastEnd) {
                continue;
            }

            String snippet = String.join(" ", Arrays.copyOfRange(words, start, end));

            for (String word : queryWords) {
                snippet = snippet.replaceAll("(?i)" + word, "<b>$0</b>");
            }

            snippets.add(snippet);
            lastEnd = end;

            if (snippets.size() >= maxSnippets) {
                break;
            }
        }

        return "... " + String.join(" ... ", snippets) + " ...";
    }

    public int getTotalCountPageWithLemma(String query, String siteUrl) {
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

    public int getTotalCountPageWithLemma(String query) {
        int pageCount = 0;
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
            pageCount += pages.size();

        }
        return pageCount;
    }

    public List<SearchData> searchAllSites(String query, int offset, int limit) {
        List<SearchData> allResults = new ArrayList<>();

        List<Site> sites = siteRepository.findAll();

        for (Site site : sites) {
            List<SearchData> siteResults = search(query, site.getUrl(), offset, limit);
            allResults.addAll(siteResults);
        }

        allResults.sort((r1, r2) -> Float.compare(r2.getRelevance(), r1.getRelevance()));

        int start = offset;
        int end = Math.min(offset + limit, allResults.size());
        return allResults.subList(start, end);
    }
}

