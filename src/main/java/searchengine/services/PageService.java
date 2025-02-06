package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.entity.Page;
import searchengine.model.entity.Site;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

@Service
@RequiredArgsConstructor
public class PageService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    public void savePage(String url, String content,Integer statusCode) {

        Site site = siteRepository.findByUrl(url)
                .orElseThrow(() -> new RuntimeException("Сайт с" + url + " не найден"));


        Page page = new Page();
        page.setPath(url);
        page.setSite(site);
        page.setContent(content);
        pageRepository.save(page);
    }


}
