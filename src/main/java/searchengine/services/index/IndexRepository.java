package searchengine.services.index;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;

import java.util.List;

@Transactional
public interface IndexRepository extends CrudRepository<Index, Integer> {

    @Modifying
    @Transactional
    @Query("delete from Index i where i.page.id = :pageId")
    void deleteIndexByPageId(@Param("pageId") int pageId);

    @Query("select i from Index i where i.page.id = :pageId")
    List<Index> getIndexesByPageId(@Param("pageId") int pageId);
}

