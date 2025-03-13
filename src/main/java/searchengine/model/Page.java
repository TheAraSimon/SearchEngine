package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "page", indexes = {
        @javax.persistence.Index(name = "idx_path_site", columnList = "path, site_id", unique = true)
})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "INT")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false, columnDefinition = "INT")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Site site;

    @Column(name = "path", nullable = false, columnDefinition = "VARCHAR(255)")
    private String path;

    @Column(name = "code", nullable = false, columnDefinition = "INT")
    private Integer code;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

}
