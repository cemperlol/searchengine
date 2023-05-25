package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "lemma")
@Getter
@Setter
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    Site site;

    @Column(nullable = false)
    String lemma;

    @Column(nullable = false)
    int frequency;
}
