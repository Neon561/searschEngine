package searchengine.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.model.SiteStatus;
import searchengine.model.entity.Site;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

//почитать про синг тон
@Service
@Data
@RequiredArgsConstructor
public class IndexService {

    public static AtomicBoolean isRunning = new AtomicBoolean(false);

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final ForkJoinPool forkJoinPool;
    private final LemmaService lemmaService;
    private final PageService pageService;


    public void startIndex(List<SiteConfig> siteConfigList) {
        // List<Site> res = new ArrayList<>();

        // это вроде должны получить из контроллера??
//        for (SiteConfig siteConfig : sitesList.getSites()) {
//            res.add(siteRepository.findByUrl(siteConfig.getUrl())
//                    .orElseGet(() -> {
//                        Site newSite = new Site();
//                        newSite.setUrl(siteConfig.getUrl());
//                        newSite.setSiteName(siteConfig.getName());
//                        newSite.setSiteStatus(SiteStatus.INDEXING);
//                        System.out.println("Добавление сайта в базу данных: " +  siteConfig.getUrl());
//                        return siteRepository.save(newSite);
//                    }));
//
//        }
        isRunning.set(true);
        try {
            try {
                deleteSites(siteConfigList);
            } catch (Exception ignored) {
            }

            List<Site> sites = saveSiteFromConfig(siteConfigList);

            sites.stream()
                    .map(site -> new IndexTask(
                            site.getUrl(),
                            lemmaService,
                            pageService,
                            site
                    ))
                    .forEach(forkJoinPool::execute);
        } catch (Exception e) {

        } finally {
            isRunning.set(false);
        }
    }


    public void deleteSites(List<SiteConfig> siteConfigList) {
        for (SiteConfig siteConfig : siteConfigList) {
            System.out.println("Удалена информация по сайтам : " + siteConfig.getUrl());
            siteRepository.deleteByUrl(siteConfig.getUrl());
        }
    }

    public void updateStatus(List<SiteConfig> siteConfigList) {
        for (SiteConfig siteConfig : siteConfigList) {
            System.out.println("Удалена информация по сайтам : " + siteConfig.getUrl());
            //todo
        }

    }

    public void savePage(String url, Long siteId) {
        //todo
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Сайт с ID " + siteId + " не найден"));

    }

    //or string name?
    private Long getSiteIdFromUrl(String url) {

        Optional<Site> site = siteRepository.findByUrl(url);

        return site.orElseThrow().getId();
    }

    private List<Site> saveSiteFromConfig(List<SiteConfig> siteConfigs) {

        List<Site> res = new ArrayList<>();
        for (SiteConfig siteConfig : siteConfigs) {
            System.out.println("Добавление сайта со статусом INDEXING " + siteConfig.getUrl());
            Site site = new Site();
            site.setUrl(siteConfig.getUrl());
            site.setSiteName(siteConfig.getName());
            site.setSiteStatus(SiteStatus.INDEXING); // Устанавливаем статус INDEXING
            site.setStatusTime(Instant.now()); // Устанавливаем время начала индексации
            site.setLastError(""); // Если нужно, установите пустое сообщение об ошибке

            res.add(siteRepository.save(site));
        }
        return res;
    }
}
