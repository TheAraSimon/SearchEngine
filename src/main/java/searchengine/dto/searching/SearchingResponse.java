package searchengine.dto.searching;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchingResponse {
    private boolean result;
    private int count;
    private List<SearchResult> data;
    private String error;
}
