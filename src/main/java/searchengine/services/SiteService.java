package searchengine.services;

import searchengine.model.SiteEntity;

public interface SiteService {

    //добавить сайт entity
    SiteEntity saveSite(SiteEntity site);

    //найти кол записей
    long count();

    //найти сайт по его url (вернуть Entity)
    SiteEntity findByUrl(String url);

    //проверить наличие сайтов у которых индексация еще идет
    boolean existIndexing();

    //удалить сайт по его id
    void delById(int id);

    //проверить наличие сайта по url
    boolean existUrl(String url);

    //удалить все
    void clear();

}
