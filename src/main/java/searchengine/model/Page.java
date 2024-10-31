package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import javax.persistence.Index;

@Entity
//@Index(name = "path", columnList = "path")
@Table(name = "page", indexes = @Index(columnList = "path"))
@Setter
@Getter
//@AllArgsConstructor
public class Page {

    @Id
    @Column(name = "id", insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    //todo association! CascadeType-????
    @ManyToOne(optional = false)
    @JoinColumn(name = "site_id")
    private Site siteId;

    @Column(name = "path", nullable = false, columnDefinition = "TEXT")
    private String path;

    @Column(name = "code", nullable = false, columnDefinition = "INT")
    private int code;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

}
