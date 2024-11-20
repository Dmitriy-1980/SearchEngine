package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;


@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    //получить id сайта по url
//    @Query(nativeQuery = true, value = "Select id FROM site WHERE url=:paramUrl LIMIT 1;")
//    SiteEntity findIdByUrl(@Param("paramUrl") String url);

    //получить всю запись сайта по его url
    @Query(nativeQuery = true, value = "SELECT * FROM site WHERE site.url=:paramUrl LIMIT 1")
    SiteEntity findByUrl(@Param("paramUrl") String siteUrl);


    //проверить наличие сайтов у которых индексация еще идет
    @Query(nativeQuery = true, value = "SELECT EXISTS (SELECT * FROM site WHERE status='INDEXING')")
    boolean existIndexing();

    //проверить наличие сайта по url
    @Query(nativeQuery = true, value = "SELECT EXISTS (SELECT * FROM site WHERE url=:paramUrl)")
    boolean existUrl(@Param("paramUrl") String siteUrl);

    //выполнить переданный запрос
//    @Query(nativeQuery = true, value = ":paramQuery")
//    List<Site> exequteQuery(@Param("paramQuery") String query);

}
