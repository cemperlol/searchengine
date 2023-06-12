package searchengine.repositories;

import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface IndexRepository extends CommonEntityRepository<Index> {

}

