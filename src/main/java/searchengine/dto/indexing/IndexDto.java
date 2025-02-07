package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexDto {
    private Integer id;
    private Integer page;
    private Integer lemma;
    private Float rank;
}
