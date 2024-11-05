package searchengine.services;


import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

public interface SiteCRUDService {

    //добавить сайт по его url
    Optional<Integer> addSite(String url, String name);

    //найти сайт по его url
    Optional<Site> getByUrl(String url);

    //найти сайт по id
    Optional<Site> getById(int id);

    //обновить сайт по его url
    boolean updateByUrl(String url, Site site);

    //обновить сайт по его id
    boolean updateById(int id, Site site);

    //удалить сайт по его url
    boolean delByUrl(String url);

    //удалить сайт по его id
    boolean delById(int id);






}
