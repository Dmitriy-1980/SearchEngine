package searchengine.services;

import searchengine.model.Page;

import java.util.Optional;

public interface PageCRUDService {

    //добавить страницу
    Optional<Integer> addPage(Page page);

    //получить страницу по id страницы
    Optional<Page> getById(int pageId);

    //обновить страницу по id
    boolean upateById(Page page);

    //удалить страницу по id
    boolean delById(int pageId);

    //удалить все страницы по id сайта
    boolean delAllBySiteId(int siteId);



}
