package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "lemma")
public class Lemma {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "site_id", nullable = false)
    int siteId;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255) NOT NULL")
    String lemma;

    @Column(name = "frequency")
    int frequency;


}
