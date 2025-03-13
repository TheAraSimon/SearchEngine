package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "index_table")
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "INT")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false, columnDefinition = "INT")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false, columnDefinition = "INT")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lemma lemma;

    @Column(name = "ranking", nullable = false, columnDefinition = "FLOAT")
    private Float rank;
}
