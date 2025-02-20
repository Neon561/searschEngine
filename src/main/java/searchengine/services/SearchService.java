package searchengine.services;
import searchengine.dto.SearchData;

import java.util.List;


public interface SearchService {
    List<SearchData> search(String query, String site, int offset, int limit);

}
