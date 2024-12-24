package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;


@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

//    //получить всю запись сайта по его url
//    @Query(nativeQuery = true, value = "SELECT * FROM site WHERE site.url=:paramUrl LIMIT 1")
//    SiteEntity findByUrl(@Param("paramUrl") String siteUrl);

//    //получить сайт (entity) по его id
//    @Query(nativeQuery = true,value = "SELECT * FROM site WHERE id=:paramId)")
//    SiteEntity findById(@Param("paramId") int siteId);

//    //проверить наличие сайтов у которых индексация еще идет
//    @Query(nativeQuery = true, value = "SELECT EXISTS (SELECT * FROM site WHERE status='INDEXING')")
//    boolean existIndexing();

//    //проверить наличие сайта по url
//    @Query(nativeQuery = true, value = "SELECT EXISTS (SELECT * FROM site WHERE url=:paramUrl)")
//    boolean existUrl(@Param("paramUrl") String siteUrl);

//    //удалить все
//    @Transactional
//    @Modifying
//    @Query(nativeQuery = true, value = "DELETE FROM site;")
//    void clear();

}
