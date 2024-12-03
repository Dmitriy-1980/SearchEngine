package searchengine.services;

import searchengine.model.LemmaEntity;

import java.util.List;
import java.util.Optional;

public interface LemmaService {

    //добавить лемму
    LemmaEntity saveLemma(LemmaEntity lemma);

    //удалить лемму по ее ID
    boolean delLemma(int id);

    //удалить лемму по ID сайта
    boolean delBySiteId(int id);

    //удалить все леммы по url сайта
    void delAllBySiteUrl(String siteUrl);

    //изменить лемму
    boolean updateLemma(LemmaEntity lemma);

    //получить лемму по ее ID (Optional)
    Optional<LemmaEntity> getOptnlById(int id);
    //получить лемму по ее ID (Entity)
    LemmaEntity getEntityById(int id);
    //получить лемму по самому слову
    LemmaEntity getEntityByLemma(String lemma);
//    //получить frequency по имени (лемме)
//    int getCountByName(String name);

    //кол лемм по указанному id сайта
    int getCountBySiteId(int id);

    //удалить все леммы по ID сайта
    void delAllBySiteId(int id);

    //удалить все
    void clear();

    //найти id лемм из запроса и выстроить по убыванию частоты
    List<Integer> getIdList(String listLemma);

    List<Integer> test(String listLemma);

}
