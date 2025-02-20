package searchengine.services;

public class NormalizerUrl {
   public static String normalizeUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
