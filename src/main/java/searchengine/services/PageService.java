package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entity.Index;
import searchengine.model.entity.Lemma;
import searchengine.model.entity.Page;
import searchengine.model.entity.Site;
import searchengine.model.repository.IndexRepository;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;

    @Transactional
    public Page savePage(String url, String content, Integer statusCode, Site site) throws URISyntaxException {



        try {
            Page page = new Page();
            page.setPath(url);
            page.setSite(site);
            page.setContent(content);
            page.setStatusCode(statusCode);
            System.out.println("Добавляем страницу в бд " + url);
            siteRepository.updateStatusTime(site.getId());
            return pageRepository.save(page);


        } catch (Exception e) {
            throw new RuntimeException("не получилось создать страницу " + url, e);
        }
    }

    @Transactional(readOnly = true)
    boolean pageExist(Long siteId, String url) {
        return pageRepository.existsBySiteIdAndPath(siteId,url);
    }

    @Transactional
    public void deletePageAndUpdateLemmas(String pageUrl) {
        Optional<Page> pageOptional = pageRepository.findByPath(pageUrl);
        if (pageOptional.isEmpty()) {
            return;
        }

        Page page = pageOptional.get();
        List<Index> indexes = indexRepository.findByPageId(page.getId());

        for (Index index : indexes) {
            Lemma lemma = index.getLemma();
            long count = indexRepository.countByLemmaId(lemma.getId());

            if (count <= 1) {
                lemmaRepository.delete(lemma);
            } else {
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmaRepository.save(lemma);
            }
        }

        indexRepository.deleteAll(indexes);
        pageRepository.delete(page);
    }

    //public boolean pageExist(Long siteId, String path) {
      //  return pageRepository.existsBySiteIdAndPath(siteId, path);
    //}
}


