package searchengine.services;


import searchengine.model.SiteEntity;

import java.util.Optional;

public interface SiteService {

    //добавить сайт по его url
    boolean addSite(String url, String name);

    //найти сайт по его url
    Optional<SiteEntity> getByUrl(String url);

    //найти сайт по id
    Optional<SiteEntity> getById(int id);

    //обновить сайт по его url
    boolean updateByUrl(String url, SiteEntity site);

    //обновить сайт по его id
    boolean updateById(int id, SiteEntity site);

    //удалить сайт по его url
    boolean delByUrl(String url);

    //удалить сайт по его id
    boolean delById(int id);

    //выполнить переданный запрос
//    ResponseEntity<List<Site>> executeQuery(String query);




}
