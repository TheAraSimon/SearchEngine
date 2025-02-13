package searchengine.dto.searching;

import lombok.Data;

import java.util.List;

@Data
public class SearchingResponse {
    private boolean result;
    private int count;
    private List<SearchData> data;
    private String error;
}
