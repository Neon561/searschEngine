package searchengine.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.SiteStatus;
import searchengine.model.entity.Site;
import searchengine.model.repository.SiteRepository;

import java.time.Instant;

@Service
@Data
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;

    public void updateSiteStatus(Site site) {
        siteRepository.updateStatus(site.getId(), SiteStatus.INDEXED, Instant.now());
    }
    public void setFailedStatus(Site site, String errorMessage) {
        siteRepository.updateStatus(site.getId(), SiteStatus.FAILED, Instant.now());
        System.out.println("Сайт " + site.getUrl() + " главная страница сайта недоступна " + errorMessage);
    }

//    public boolean isIndexingComplete() {
//        return siteRepository.countByStatus(SiteStatus.INDEXING) == 0;
//    }
}
