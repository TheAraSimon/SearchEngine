package searchengine.dto.indexing;

import lombok.Data;
import searchengine.model.Site;

@Data
public class PageDto {
    private Integer id;
    private Integer site;
    private String path;
    private Integer code;
    private String content;
}
