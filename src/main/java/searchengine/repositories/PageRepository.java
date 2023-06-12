package searchengine.repositories;

import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface PageRepository extends CommonEntityRepository<Page> {

}
