package searchengine.controllers;

import lombok.RequiredArgsConstructor;
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

//    public ApiController(StatisticsService statisticsService) {
//        this.statisticsService = statisticsService;
//    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {

        return ResponseEntity.ok(statisticsService.getStatistics());

    }

    @GetMapping("/startIndexing")
    public ResponseEntity<SuccessfulIndexingResult> startIndex() {
//        if (!IndexService.isRunning.getAndSet(true)) {
//            // todo
//            indexService.startIndex(sitesList.getSites());
//            return ResponseEntity.ok(IndexingResult.successfulResult());
//        }
//        return ResponseEntity.ok(IndexingResult.failResult("Индексация уже запущена"));
        if (!IndexService.isRunning.getAndSet(true)) {
            indexService.startIndex(sitesList.getSites());
            return ResponseEntity.ok(new SuccessfulIndexingResult());

        }
        throw new RuntimeException("Индексация уже запущена");
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResult> stopIndex() {
        try {
            //todo запрашивать стоп индекс
            return null;
        } catch (Exception e) {
            return ResponseEntity.ok(IndexingResult.successfulResult());
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

        if (false){//todo !isIndexingComplete()) {
            throw new RuntimeException(("Индексация еще не завершена"));
        }

        try {
            List<SearchData> results;
            int totalCount;

            if (site == null || site.trim().isEmpty()) {
                // Если сайт не указан, ищем по всем сайтам
                results = searchService.searchAllSites(query, offset, limit);
                totalCount = searchService.getTotalCountPageWithLemma(query);
            } else {
                // Ищем по конкретному сайту
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
        // Проверяем, принадлежит ли страница сайтам в конфигурации
        if (!indexService.isUrlInConfig(url)) {
            return ResponseEntity.ok(IndexingResult.failResult("Данная страница находится за пределами сайтов, указанных в конфигурационном файле"));
        }

        // Запускаем удаление старой страницы и индексацию новой
        indexService.reindexPage(url);

        return ResponseEntity.ok(IndexingResult.successfulResult());
    }


}
