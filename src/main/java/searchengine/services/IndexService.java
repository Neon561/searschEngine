package searchengine.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.model.SiteStatus;
import searchengine.model.entity.Site;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

import static searchengine.services.NormalizerUrl.normalizeUrl;

//почитать про сингл тон
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
    private final SiteService siteService;


        public void startIndex(List<SiteConfig> siteConfigList) {
            isRunning.set(true);
            try {

                List<Site> sites = saveSiteFromConfig(siteConfigList);

                for (Site site : sites) {
                    if (!isSiteAvailable(site.getUrl())) {
                        siteService.setFailedStatus(site, "Главная страница недоступна");
                        continue; // Пропускаем индексацию
                    }
                }
                // Создаём список задач
                List<IndexTask> tasks = sites.stream()
                        .map(site -> new IndexTask(
                                site.getUrl(),
                                lemmaService,
                                pageService,
                                siteService,
                                site
                        ))
                        .toList();

                // Запускаем все задачи и ждём их завершения
                tasks.forEach(forkJoinPool::invoke);
                System.out.println("Индексация завершена");

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isRunning.set(false);
            }
        }
    public void stopIndexing() {
        isRunning.set(false);
        forkJoinPool.shutdownNow();// Прерываем все задачи в пуле
        System.out.println("индексация прервана");
    }
    private boolean isSiteAvailable(String url) {
        try {

            Connection.Response connection = Jsoup.connect(url)
                    .timeout(5000)  // Таймаут 5 сек
                    .ignoreHttpErrors(true)
                    .execute();


            return connection.statusCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    //or string name?
    private Long getSiteIdFromUrl(String url) {

        Optional<Site> site = siteRepository.findByUrl(url);

        return site.orElseThrow().getId();
    }

    public void reindexPage(String url) {
        String normalizeUrl = normalizeUrl(url);

        Optional<Site> site = siteRepository.findAll().stream()
                .filter(s -> normalizeUrl.startsWith(s.getUrl()))
                        .findFirst();

        if (site.isEmpty()) {
            throw new RuntimeException("Сайт для URL " + url + " не найден в базе данных.");
        }
        // Определяем, к какому сайту принадлежит страница

        // Удаляем старую страницу перед индексацией
       //pageService.deletePageByPath(normalizeUrl);
        pageService.deletePageAndUpdateLemmas(normalizeUrl);

        // Создаём задачу для индексации этой страницы
        IndexTask task = new IndexTask(normalizeUrl, lemmaService, pageService,siteService, site.get());

        // Запускаем индексацию в ForkJoinPool
        forkJoinPool.invoke(task);
    }


    public boolean isUrlInConfig(String url) {
        return sitesList.getSites().stream()
                .anyMatch(siteConfig -> url.startsWith(siteConfig.getUrl()));
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
            site.setLastError("");

            res.add(siteRepository.save(site));
        }
        return res;
    }

}
