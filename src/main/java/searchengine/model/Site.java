package searchengine.model;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @Column(columnDefinition = "ENUM('INDEXING','INDEXED','FAILED')", nullable = false)
    private SiteStatus status;

    @Column(name = "status_time", nullable = false)
    private Timestamp statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(mappedBy = "site")
    private Set<Page> pages = new HashSet<>();

    /* @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "index",
            joinColumns = {@JoinColumn(name = "site_id")},
            inverseJoinColumns = {@JoinColumn(name = "lemma_id")}
    )
    List<Lemma> lemmas = new ArrayList<>(); */
}
