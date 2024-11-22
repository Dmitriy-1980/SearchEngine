package searchengine.services;

import searchengine.model.LemmaEntity;

import java.util.Optional;

public interface LemmaService {

    //добавить лемму
    boolean addLemma(LemmaEntity lemma);

    //удалить лемму по ее ID
    boolean delLemma(int id);

    //удалить лемму по ID сайта
    boolean delBySiteId(int id);

    //изменить лемму
    boolean updateLemma(LemmaEntity lemma);

    //получить лемму по ее ID (Optional)
    Optional<LemmaEntity> getOptnlById(int id);
    //получить лемму по ее ID (Entity)
    LemmaEntity getEntityById(int id);

    //кол лемм по указанному id сайта
    int getCountBySiteId(int id);

    //удалить все леммы по ID сайта
    void delAllBySiteId(int id);
}
