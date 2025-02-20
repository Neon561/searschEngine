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


    public IndexTask(String url, LemmaService lemmaService, PageService pageService, Site site) {
        this.url = url;
        this.lemmaService = lemmaService;
        this.pageService = pageService;
        this.site = site;
    }

//    lemma
//    id INT NOT NULL AUTO_INCREMENT;
//    site_id INT NOT NULL — ID веб-сайта из таблицы site;
//    lemma VARCHAR(255) NOT NULL — нормальная форма слова (лемма);
//    frequency INT NOT NULL — количество страниц, на которых слово встречается хотя бы один раз. Максимальное значение не может превышать общее количество слов на сайте.


    @Override
    protected void compute() {

        try {
            if (pageService.pageExist(url)) {
                return;
            }//todo вынести задержку в константу и таймайут и слееп
            Thread.sleep(500 + new Random().nextInt(3500)); // 500–4000 мс

            Connection connection = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(10000);

            Connection.Response response = connection.execute();

            int statusCode = response.statusCode();

            if (statusCode == 200) {
                Document doc = connection.get();
                String pageHtmlText = doc.html();

                Page savedPage = pageService.savePage(url, pageHtmlText, statusCode, site);
                lemmaService.searchAndSaveLemma(doc.body().text(), savedPage);

                List<IndexTask> subTasks = doc.select("a[href]").stream()
                        .map(link -> link.attr("abs:href"))
                        //.filter(link -> isSameDomain(url, link))// вместо след.щей строчки можно сделать проверку по хосту
                        .filter(link -> link.startsWith(url))
                        .filter(link -> !link.matches(EXCLUDE_PATTERN))
                        .distinct()

                        .map(NormalizerUrl::normalizeUrl)
                        .map(link -> new IndexTask(link, lemmaService, pageService, site))
                        .toList();

                System.out.println("Запустили таску для " + url);

                invokeAll(subTasks);


            } else {
                System.out.println("Страница не проиндексирована " + url + "статус код" + statusCode);
            }

        } catch (Exception e) {
            System.err.println("Ошибка при обработке страницы " + url + ": " + e.getMessage());
            e.printStackTrace();
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
//     String normalizeUrl(String url) {
//        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
//    }
}
