package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import searchengine.model.AbstractEntity;

@NoRepositoryBean
public interface CommonEntityRepository<E extends AbstractEntity> extends JpaRepository<E, Integer> {
}
