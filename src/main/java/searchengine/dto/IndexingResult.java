package searchengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
public class IndexingResult {

    final private boolean result;
    @JsonInclude(Include.NON_NULL)
    final private String error;

    public static IndexingResult successfulResult() {
        return new IndexingResult(true, null);
    }

    public static IndexingResult failResult(String error) {
        return new IndexingResult(false, error);
    }
}
