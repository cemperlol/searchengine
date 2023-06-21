package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "lemma")
@Getter @Setter
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @ManyToMany(mappedBy = "pageLemmas", fetch = FetchType.LAZY)
    Set<Page> lemmaPages = new HashSet<>();

    @OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY)
    private Set<Index> indexes;
}
