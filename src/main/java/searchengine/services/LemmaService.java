package searchengine.services;

import searchengine.model.LemmaEntity;

public interface LemmaService {

    //добавить лемму
    LemmaEntity saveLemma(LemmaEntity lemma);

    //удалить все леммы по url сайта
    void delAllBySiteUrl(String siteUrl);

    //кол лемм по указанному id сайта
    int getCountBySiteId(int id);

    //кол записей
    long count();

    //удалить все
    void clear();

    //обновление записи по самой лемме
    LemmaEntity update(String lemma, int count);


}
