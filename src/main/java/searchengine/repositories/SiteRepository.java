package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.util.Optional;

@Repository
public interface SiteRepository extends CommonEntityRepository<Site> {

    @Modifying
    @Transactional
    @Query("update Site s set s.statusTime = current_timestamp where s.id = :id")
    void updateStatusTime(@Param("id") int id);

    @Modifying
    @Transactional
    @Query("update Site s set s.status = :status, s.statusTime = current_timestamp where s.id = :id")
    void updateStatus(@Param("status") SiteStatus status, @Param("id") int id);

    @Modifying
    @Transactional
    @Query("update Site s set s.lastError = :lastError, s.status = 'FAILED', s.statusTime = current_timestamp " +
            "where s.id = :id")
    void updateLastError(@Param("id") int id, @Param("lastError") String lastError);

    @Query("select s from Site s where s.url = :url")
    Optional<Site> findByUrl(@Param("url") String url);
}
