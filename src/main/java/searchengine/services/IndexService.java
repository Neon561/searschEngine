package searchengine.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.model.SiteStatus;
import searchengine.model.entity.Site;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@Service
@Data
@RequiredArgsConstructor
public class IndexService {

    private static boolean isRunning;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final ForkJoinPool forkJoinPool;
    private final LemmaService lemmaService;
    private final PageService pageService;


    public void startIndex(List<SiteConfig> siteConfigList) {
        isRunning = true;
        deleteSites(siteConfigList);
        saveSiteFromConfig(siteConfigList);

                siteConfigList.stream()
                .map((SiteConfig siteConfig) -> new IndexTask(siteConfig.getUrl(), lemmaService,pageService,getSiteIdFromUrl(siteConfig.getUrl())))
                .forEach(forkJoinPool::execute);
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
    private Long getSiteIdFromUrl(String url){

        Optional<Site> site = siteRepository.findByUrl(url);

        return site.orElseThrow().getId();
    }

    private void saveSiteFromConfig(List<SiteConfig> siteConfigs) {


        for (SiteConfig siteConfig : siteConfigs) {
            System.out.println("Добавление сайта {} со статусом INDEXING" + siteConfig.getUrl());
            Site site = new Site();
            site.setUrl(siteConfig.getUrl());
            site.setSiteName(siteConfig.getName());
            site.setSiteStatus(SiteStatus.INDEXING); // Устанавливаем статус INDEXING
            site.setStatusTime(Instant.now()); // Устанавливаем время начала индексации
            site.setLastError(""); // Если нужно, установите пустое сообщение об ошибке

            siteRepository.save(site);
        }
    }
}
