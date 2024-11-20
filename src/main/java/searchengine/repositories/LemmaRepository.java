package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    //кол лемм по указанному id сайта
    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM lemma WHERE site_id=:paramId")
    int getCountBySitrId(@Param("paramId") int id);

    //удалить все леммы по ID сайта
    @Query(nativeQuery = true, value = "DELETE FROM lemma WHERE site_id=:paramId")
    void delAllBySiteId(@Param("paramId") int id);

}
