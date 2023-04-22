package searchengine.model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //@OneToMany
    int id;

    @Column(columnDefinition = "ENUM('INDEXING','INDEXED','FAILED')")
    String status;

    @Column(name = "status_time")
    Timestamp statusTime;

    @Column(name = "last_error")
    String lastError;

    String url;

    String name;
}
