package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    //удалить всех по siteId
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM search_index WHERE page_id IN (SELECT id FROM page WHERE id=:paramId);")
    void delAllBySiteId(@Param("paramId") int siteId);

    //удалить всех по Url сайла
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM search_index WHERE page_id IN (SELECT id FROM page WHERE path=:paramUrl);")
    void delAllBySiteUrl(@Param("paramUrl") String siteUrl);

    //удалить все
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM search_index;")
    void clear();


}
