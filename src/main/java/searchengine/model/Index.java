package searchengine.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "serch_index")
public class Index {

    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "page_id")
    int pageId;

    @Column(name = "lemma_id")
    int lemmaId;

    @Column(name = "rank", columnDefinition = "FLOAT")
    float rank;

}
