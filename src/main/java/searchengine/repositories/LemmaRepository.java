package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import java.util.List;


@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    //найти по номеру сайта и лемме
    LemmaEntity findBySiteIdAndLemma(int siteId , String lemma);

    //получить список id по лемме (слову)
    List<LemmaEntity> findAllByLemma(String lemma);

    //кол. лемм по id сайта
    int countBySiteId(int siteId);

    //удалить леммы по id сайта
    void deleteAllBySiteId(int siteId);

    //получить все леммы сайта по его id
    List<LemmaEntity> getBySiteId(int siteId);

}
