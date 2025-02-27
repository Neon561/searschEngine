package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.IndexingResult;
import searchengine.dto.SearchData;
import searchengine.dto.SearchResponse;
import searchengine.dto.SuccessfulIndexingResult;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexService;
import searchengine.services.SearchServiceImpl;
import searchengine.services.StatisticsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private final IndexService indexService;
    private final SitesList sitesList;

    private final StatisticsService statisticsService;
    private final SearchServiceImpl searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {

        return ResponseEntity.ok(statisticsService.getStatistics());

    }

    @GetMapping("/startIndexing")
    public ResponseEntity<SuccessfulIndexingResult> startIndex() {
        try {
            indexService.startIndex(sitesList.getSites());
            return ResponseEntity.ok(new SuccessfulIndexingResult());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SuccessfulIndexingResult());
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResult> stopIndex() {
        try {
            if (IndexService.isRunning.get()){
                indexService.stopIndexing();
            }

            return ResponseEntity.ok(IndexingResult.successfulResult());
        } catch (Exception e) {
            throw new RuntimeException("индексация не запущена");
        }

    }


    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "20") Integer limit) {

        if (query == null || query.trim().isEmpty()) {
            throw  new RuntimeException("Указан пустой поисковый запрос");
        }

        if (IndexService.isRunning.get()){
            throw new RuntimeException(("Индексация еще не завершена"));
        }

        try {
            List<SearchData> results;
            int totalCount;

            if (site == null || site.trim().isEmpty()) {
                results = searchService.searchAllSites(query, offset, limit);
                totalCount = searchService.getTotalCountPageWithLemma(query);
            } else {
                results = searchService.search(query, site, offset, limit);
                totalCount = searchService.getTotalCountPageWithLemma(query, site);
            }

            return ResponseEntity.ok(SearchResponse.success(totalCount, results));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка во время поиска", e);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResult> addOrUpdatePage(@RequestParam String url) {
        if (!indexService.isUrlInConfig(url)) {
            return ResponseEntity.ok(IndexingResult.failResult("Данная страница находится за пределами сайтов, указанных в конфигурационном файле"));
        }

        indexService.reindexPage(url);

        return ResponseEntity.ok(IndexingResult.successfulResult());
    }


}
