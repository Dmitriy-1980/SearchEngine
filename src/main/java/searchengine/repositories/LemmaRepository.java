package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    //проверить наличие леммы
    @Query(nativeQuery = true, value = "SELECT EXISTS (SELECT * FROM lemma WHERE lemma=:paramLemma);")
    boolean existLemma(@Param("paramLemma") String lemma);

    //получить сущность по id
    @Query(nativeQuery = true, value = "SELECT * FROM lemma WHERE id=:paramId;")
    List<LemmaEntity> getEntityById(@Param("paramId") int lemmaId);


    //кол лемм по указанному id сайта
    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM lemma WHERE site_id=:paramId")
    int getCountBySiteId(@Param("paramId") int id);


    //удалить все леммы по ID сайта
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM lemma WHERE site_id=:paramId")
    void delAllBySiteId(@Param("paramId") int id);

    //удалить все леммы по названию сайта
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM lemma WHERE site_id IN (SELECT id FROM site WHERE url=:paramUrl)")
    void delAllBySiteUrl(@Param("paramUrl") String url);

    //удалить все
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM lemma;")
    void clear();

    //получить id по слову
    @Query(nativeQuery = true, value = "SELECT * FROM lemma WHERE lemma = :paramLemma")
    List<LemmaEntity> getEntityByLemma(@Param("paramLemma") String lemma);

}
