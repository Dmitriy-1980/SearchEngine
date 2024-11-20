package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "search_index")
@Getter
@Setter
public class IndexEntity {

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
