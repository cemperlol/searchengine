package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "index")
@Getter
@Setter
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    int id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    Lemma lemma;

    @Column(nullable = false)
    float rank;
}
