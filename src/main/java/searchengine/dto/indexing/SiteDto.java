package searchengine.dto.indexing;

import lombok.Data;
import searchengine.model.Status;

import java.time.Instant;

@Data
public class SiteDto {
    private Integer id;
    private Status status;
    private Instant statusTime;
    private String lastError;
    private String url;
    private String name;
}
