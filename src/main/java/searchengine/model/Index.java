package searchengine.model;

import javax.persistence.*;

@Entity
@Table(name = "index")
public class Index {

    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "page_id")
    @OneToOne()
    Page page;

    int lemmaId;

    int rank;

}
