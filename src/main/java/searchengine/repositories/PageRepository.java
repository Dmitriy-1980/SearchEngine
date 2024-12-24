package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

//    //кол страниц по заданному id сайта
//    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM page WHERE site_id=:paramId;")
//    int getCountBySiteId(@Param("paramId") int id);

    //удаление всех страниц по Id сайта
//    @Transactional
//    @Modifying
//    @Query(nativeQuery = true, value = "DELETE FROM page WHERE site_id=:paramId")
//    void delAllBySiteId(@Param("paramId") int id);

//    //проверить наличие по path
//    @Query(nativeQuery = true, value = "SELECT EXISTS (SELECT * FROM page WHERE site_id=:paramSiteId AND path=:paramPath)")
//    boolean existUrlWithSite(@Param("paramSiteId") int siteId, @Param("paramPath") String path );
//
//    //удалить все
//    @Transactional
//    @Modifying
//    @Query(nativeQuery = true, value = "DELETE FROM page;")
//    void clear();


}
