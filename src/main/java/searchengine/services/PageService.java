package searchengine.services;

import searchengine.model.PageEntity;
import java.util.List;

public interface PageService {

    //кол страниц по заданному id сайта
    int getCountBySiteId(int id);

    //кол записей
    long count();

    //сохранить странцу как сущность
    PageEntity savePage(PageEntity page);

    //удалить все страницы по id сайта
    void delAllBySiteId(int siteId);

    //удалить все
    void clear();

    //отфильтровать список page_id по заданному сайту (те убрать страницы не с указанного сайта)
    List<Integer> filterPageIdListBySite(List<Integer> pageIdList, String url);

    //получить страницу по id
    PageEntity getPage(int id);
}
