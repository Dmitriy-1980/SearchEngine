package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lemma")
public class LemmaEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "site_id", nullable = false)
    int siteId;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255) NOT NULL")
    String lemma;

    @Column(name = "frequency")
    float frequency;


}
