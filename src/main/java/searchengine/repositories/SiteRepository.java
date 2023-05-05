package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

public interface SiteRepository extends CrudRepository<Site, Integer> {

    @Modifying
    @Transactional
    @Query("update Site s set s.statusTime = current_timestamp where s.id = :id")
    void updateSiteStatusTime(@Param("id") int id);
}
