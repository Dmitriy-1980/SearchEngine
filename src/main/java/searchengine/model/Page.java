package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity

@Table(name = "page")
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


    @Column(name = "path", nullable = false, columnDefinition = "VARCHAR(255)")
    private String path;

    @Column(name = "code", nullable = false, columnDefinition = "INT")
    private int code;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

}
