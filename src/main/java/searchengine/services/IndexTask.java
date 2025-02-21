package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.entity.Page;
import searchengine.model.entity.Site;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.RecursiveAction;


public class IndexTask extends RecursiveAction {
    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) hellionBot_561 (HTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36";
    private final String referrer = "http://www.google.com";

    private final String url;
    private final LemmaService lemmaService;
    private final PageService pageService;

    private final Site site;
    private static final String EXCLUDE_PATTERN = ".*(\\.(png|jpg|jpeg|gif|bmp|zip|sql|pdf|doc|docx|xls|xlsx|ppt|pptx|exe|tar|gz|rar|7z)|#.*)$";
    private static final int MIN_TIME_DELAY = 500;
    private static final int MAX_TIME_DELAY = 4000;

    public IndexTask(String url, LemmaService lemmaService, PageService pageService, Site site) {
        this.url = url;
        this.lemmaService = lemmaService;
        this.pageService = pageService;
        this.site = site;
    }



    @Override
    protected void compute() {

        try {
            if (pageService.pageExist(site.getId(), extractPathFromUrl(url))) {
                return;
            }
            Thread.sleep(MIN_TIME_DELAY + new Random().nextInt(MAX_TIME_DELAY));

            Connection connection = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(10000);

            Connection.Response response = connection.execute();

            int statusCode = response.statusCode();

            if (statusCode == 200) {
                Document doc = connection.get();
                String pageHtmlText = doc.html();

                Page savedPage = pageService.savePage(extractPathFromUrl(url), pageHtmlText, statusCode, site);
                lemmaService.searchAndSaveLemma(doc.body().text(), savedPage);

                List<IndexTask> subTasks = doc.select("a[href]").stream()
                        .map(link -> link.attr("abs:href"))
                        .filter(link -> link.startsWith(url))
                        .filter(link -> !link.matches(EXCLUDE_PATTERN))
                        .distinct()

                        .map(NormalizerUrl::normalizeUrl)
                        .map(link -> new IndexTask(link, lemmaService, pageService, site))
                        .toList();

                System.out.println("Обработано: " + url + " найдено страниц: " + subTasks.size());
                invokeAll(subTasks);


            } else {
                System.out.println("Страница не проиндексирована " + url + "статус код" + statusCode);
            }

        } catch (Exception e) {
            System.err.println("Ошибка при обработке страницы " + url + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractPathFromUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Некорректный URL: " + url, e);
        }
    }

}
