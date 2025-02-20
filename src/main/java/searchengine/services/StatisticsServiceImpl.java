package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteStatus;
import searchengine.model.entity.Site;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final Random random = new Random();
    private final SitesList sites;

  //  @Override
    public StatisticsResponse getStatistics2() {
        String[] statuses = {"INDEXED", "FAILED", "INDEXING"};
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteConfig> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            SiteConfig siteConfig = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteConfig.getName());
            item.setUrl(siteConfig.getUrl());
            int pages = random.nextInt(1_000);
            int lemmas = pages * random.nextInt(1_000);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(statuses[i % 3]);
            item.setError(errors[i % 3]);
            item.setStatusTime(System.currentTimeMillis() -
                    (random.nextInt(10_000)));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    @Override
    public StatisticsResponse getStatistics() {
        final List<Site> siteList = siteRepository.findAll();
        int pagesCount = (int) pageRepository.count();
        int lemmaCount = (int) lemmaRepository.countDistinctLemmas();

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteList.size());
        total.setPages(pagesCount);
        total.setLemmas(lemmaCount);
        total.setIndexing(isIndexing(siteList));
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();

        data.setTotal(total);
        data.setDetailed(getDtailedStatisticsItemList(siteList));
        response.setStatistics(data);
        response.setResult(true);

        return response;
    }

    public boolean isIndexing(List<Site> siteList) {

        for (Site site : siteList) {
            if (site.getSiteStatus().equals(SiteStatus.INDEXING)) {
                return true;
            }
        }
        return false;
    }

    public List<DetailedStatisticsItem> getDtailedStatisticsItemList(List<Site> siteList) {
        List<DetailedStatisticsItem> res = new ArrayList<>();

        for (Site site : siteList) {
            DetailedStatisticsItem detailed = new DetailedStatisticsItem();
            detailed.setName(site.getSiteName());
            detailed.setUrl(site.getUrl());
            detailed.setStatus(site.getSiteStatus().name());
            detailed.setStatusTime(site.getStatusTime().toEpochMilli());
//или вот так ибо не совсем понятно как фронт высчитывает время
//               Instant instant = siteList.get(i).getStatusTime();
//               LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
//               DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//               String formattedTime = localDateTime.format(formatter);
//               detailed.setStatusTime(formattedTime);
            detailed.setPages(pageRepository.countBySiteId(site.getId()));
            detailed.setError(site.getLastError());
            res.add(detailed);
        }
        return res;
    }
}
