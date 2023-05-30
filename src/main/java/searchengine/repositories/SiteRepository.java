package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.util.Optional;

public interface SiteRepository extends CrudRepository<Site, Integer> {

    @Modifying
    @Transactional
    @Query("update Site s set s.statusTime = current_timestamp where s.id = :id")
    void updateStatusTime(@Param("id") int id);

    @Modifying
    @Transactional
    @Query("update Site s set s.status = :status where s.id = :id")
    void updateStatus(@Param("status") SiteStatus status, @Param("id") int id);

    @Modifying
    @Transactional
    @Query("update Site s set s.lastError = :lastError where s.id = :id")
    void updateLastError(@Param("lastError") String lastError, @Param("id") int id);

    @Query("select s from Site s where s.url = :url")
    Optional<Site> findByUrl(@Param("url") String url);

    @Query("select count(s) from Site s")
    Integer totalCount();

    @Modifying
    @Transactional
    @Query(value = "delete from site", nativeQuery = true)
    void deleteAllInBatches(@Param("batchSize") int batchSize);
}
