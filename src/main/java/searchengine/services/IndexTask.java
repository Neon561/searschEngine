package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
    // private final Long siteId;
    private final Site site;
    private static final String EXCLUDE_PATTERN = ".*(\\.(png|jpg|jpeg|gif|bmp|zip|sql|pdf|doc|docx|xls|xlsx|ppt|pptx|exe|tar|gz|rar|7z)|#.*)$";


    public IndexTask(String url, LemmaService lemmaService, PageService pageService, Site site) {
        this.url = url;
        this.lemmaService = lemmaService;
        this.pageService = pageService;
        //this.siteId = siteId;
        this.site = site;
    }
//PAGE
//id INT NOT NULL AUTO_INCREMENT;
//    site_id INT NOT NULL — ID веб-сайта из таблицы site;// todo ++
//    path TEXT NOT NULL — адрес страницы от корня сайта (должен начинаться со слэша, например: /news/372189/); //todo ++
//    code INT NOT NULL — код HTTP-ответа, полученный при запросе страницы (например, 200, 404, 500 или другие);todo //++
//    content MEDIUMTEXT NOT NULL — контент страницы (HTML-код). todo ++

//    lemma
//    id INT NOT NULL AUTO_INCREMENT;
//    site_id INT NOT NULL — ID веб-сайта из таблицы site;
//    lemma VARCHAR(255) NOT NULL — нормальная форма слова (лемма);
//    frequency INT NOT NULL — количество страниц, на которых слово встречается хотя бы один раз. Максимальное значение не может превышать общее количество слов на сайте.


    @Override
    protected void compute() {

        try {
            Thread.sleep(500 + new Random().nextInt(3500)); // 500–4000 мс
            // Подключение к странице
            Connection connection = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(10000);

            Connection.Response response = connection.execute();

            int statusCode = response.statusCode();
            System.out.println(("статус код: " + statusCode + " " + url));
            if (statusCode == 200) {
                Document doc = connection.get();
                String text = doc.body().text();
                pageService.savePage(url, text, statusCode);
                lemmaService.searchLemma(text, site); // Передаем Site

                List<IndexTask> subTasks = doc.select("a[href]").stream()
                        .map(link -> link.attr("abs:href"))
                        .filter(link -> isSameDomain(url, link))
                        .filter(link -> !link.matches(EXCLUDE_PATTERN))
                        .distinct()
                        .map(link -> new IndexTask(link, lemmaService, pageService, site))
                        .toList();

                System.out.println("Запустили таску для " + url);

                invokeAll(subTasks);

            } else {

            }

            //todo

        } catch (Exception e) {
        }
    }


    private boolean isSameDomain(String siteUrl, String pageUrl) {
        try {
            URI siteUri = new URI(siteUrl);
            URI pageUri = new URI(pageUrl);

            String siteHost = siteUri.getHost();
            String pageHost = pageUri.getHost();

            if (siteHost == null || pageHost == null) {
                return false;
            }

            return pageHost.equals(siteHost) || pageHost.endsWith("." + siteHost);
        } catch (URISyntaxException e) {
            System.out.println("Ошибка разбора URL: " + e.getMessage());
            return false;
        }
    }
}
