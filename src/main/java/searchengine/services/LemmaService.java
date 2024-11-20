package searchengine.services;

import searchengine.model.LemmaEntity;

import java.util.Optional;

//@Service
public interface LemmaService {

    //добавить лемму
    boolean addLemma(LemmaEntity lemma);

    //удалить лемму по ее ID
    boolean delLemma(int id);

    //удалить лемму по ID сайта
    boolean delBySiteId(int id);

    //изменить лемму
    boolean updateLemma(LemmaEntity lemma);

    //получить лемму по ее ID
    Optional<LemmaEntity> getById(int id);
}
