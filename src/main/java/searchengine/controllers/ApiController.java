package searchengine.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.IndexingResult;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexService;
import searchengine.services.StatisticsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private final IndexService indexService;
    private final SitesList sitesList;

    private final StatisticsService statisticsService;

//    public ApiController(StatisticsService statisticsService) {
//        this.statisticsService = statisticsService;
//    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {

        //todo вернуть объект  'statistics': {
//            "total": {
//                "sites": 10,
//                        "pages": 436423,
//                        "lemmas": 5127891,
//                        "indexing": true
//            },
//            "detailed": [
//            {
//                "url": "http://www.site.com",
//                    "name": "Имя сайта",
//                    "status": "INDEXED",
//                    "statusTime": 1600160357,
//                    "error": "Ошибка индексации: главная
//                страница сайта недоступна",
//                "pages": 5764,
//                    "lemmas": 321115
//            }
try {
    throw new RuntimeException("unable to connect DB");
}catch (Exception e){
  //  return ResponseEntity.ok(IndexingResult.failResult("Индексация еще не завершена"));
}
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResult> startIndex() {
        if (true) {
            // todo
            indexService.startIndex(sitesList.getSiteConfigs());
            return ResponseEntity.ok(IndexingResult.failResult("Индексация уже запущена"));
        }
        return ResponseEntity.ok(IndexingResult.successfulResult());


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
    public ResponseEntity<IndexingResult> search(@RequestParam String query,
                                                 @RequestParam String site,
                                                 @RequestParam(defaultValue = "0") Integer offset,
                                                 @RequestParam(defaultValue = "20") Integer limit) {
        if (query == null||query.trim().isEmpty()){
            return ResponseEntity.ok(IndexingResult.failResult("Указан пустой поисковый запрос"));

        }if (true){
            //todo
            return ResponseEntity.ok(IndexingResult.failResult("Индексация еще не завершена"));

        }
        try {
            //todo
            return null;
        } catch (Exception e) {
            return ResponseEntity.ok(IndexingResult.successfulResult());
        }

    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResult> addOrUpdatePage(@RequestParam String url) {
        if (true) {
            //todo Данная страница находится за пределами сайтов,указанных в конфигурационном файле
            return ResponseEntity.ok(IndexingResult.failResult("Данная страница находится за пределами сайтов,указанных в конфигурационном файле"));
        }
        return ResponseEntity.ok(IndexingResult.successfulResult());
    }




}
