package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "lemma")
@Getter @Setter
public class Lemma extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @ManyToMany(mappedBy = "pageLemmas")
    Set<Page> lemmaPages = new HashSet<>();

    @OneToMany(mappedBy = "lemma")
    private Set<Index> indexes;
}
