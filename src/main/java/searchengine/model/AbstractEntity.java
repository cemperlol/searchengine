package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Getter @Setter
public abstract class AbstractEntity {

    @Id
    @GeneratedValue
    @Column(nullable = false)
    private int id;
}
