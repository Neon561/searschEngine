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
            detailed.setLemmas(siteRepository.countLemmasBySiteId(site.getId()));
            res.add(detailed);
        }
        return res;
    }
}
