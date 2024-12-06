package searchengine.services;

import searchengine.model.PageEntity;

public interface PageService {

    //кол страниц по заданному id сайта
    int getCountBySiteId(int id);

    //кол записей
    long count();

    //сохранить странцу как сущность
    PageEntity addEntity(PageEntity page);

    //удалить все страницы по id сайта
    boolean delAllBySiteId(int siteId);

    //удалить все
    void clear();
}
