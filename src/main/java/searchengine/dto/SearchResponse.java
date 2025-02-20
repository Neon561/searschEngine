package searchengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchResponse {
    private boolean result;
    private int count;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<SearchData> data;

    public static SearchResponse success(int count, List<SearchData> data) {
        return new SearchResponse(true, count, data);
    }

}
