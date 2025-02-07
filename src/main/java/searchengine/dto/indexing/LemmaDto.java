package searchengine.dto.indexing;

import lombok.Data;

@Data
public class LemmaDto {
    private Integer id;
    private Integer site;
    private String lemma;
    private Integer frequency;
}
