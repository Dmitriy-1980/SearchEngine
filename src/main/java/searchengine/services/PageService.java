package searchengine.services;

import searchengine.model.PageEntity;

import java.util.Optional;

public interface PageService {

    //добавить страницу
    Optional<PageEntity> addPage(PageEntity page);

    //получить страницу по id страницы
    Optional<PageEntity> getById(int pageId);

    //обновить страницу
    boolean update(PageEntity page);

    //удалить страницу по id
    boolean delById(int pageId);

    //удалить все страницы по id сайта
    boolean delAllBySiteId(int siteId);

    //кол страниц по заданному id сайта
    Integer getCountBySiteId(int id);

    //проверить наличие по path
    boolean existUrlWithSite(int siteId, String path );
}
