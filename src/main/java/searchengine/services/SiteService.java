package searchengine.services;

import searchengine.model.SiteEntity;

import java.util.Optional;

public interface SiteService {

    //добавить сайт по его url
    boolean addSite(String url, String name);

    //найти сайт по его url (вернуть Optional)
    Optional<SiteEntity> getOptnlByUrl(String url);
    //найти сайт по его url (вернуть Entity)
    SiteEntity getEntityByUrl(String url);

    //найти сайт по id (вернуть Optional)
    Optional<SiteEntity> getOptnlById(int id);
    //найти сайт по id (вернуть Entity)
    SiteEntity getEntityById(int id);

    //обновить сайт по его url
    boolean updateByUrl(String url, SiteEntity site);

    //обновить сайт по его id
    boolean updateById(int id, SiteEntity site);

    //удалить сайт по его url
    boolean delByUrl(String url);

    //удалить сайт по его id
    boolean delById(int id);

    //проверить наличие сайтов у которых индексация еще идет
    boolean existIndexing();

    //проверить наличие сайта по url
    boolean existUrl(String url);

}
