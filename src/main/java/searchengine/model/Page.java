package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Indexed;

import javax.persistence.*;
import javax.persistence.Index;

@Entity

@Table(name = "page", indexes = {@Index(columnList = "path" , name = "index_path")})
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
//    @Index(name = "index_path", columnList = "path")
    private String path;

    @Column(name = "code", nullable = false, columnDefinition = "INT")
    private int code;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

}
