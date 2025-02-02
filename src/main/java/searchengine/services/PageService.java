package searchengine.services;

import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

public interface PageService {

    //наличие страницы по url
    boolean isExistUrl(String url);

    //кол страниц по заданному id сайта
    int getCountBySiteId(int id);

    //кол записей
    long count();

    //сохранить странцу как сущность
    PageEntity savePage(PageEntity page);

    //удалить все страницы по сайту
    void delAllBySiteId(SiteEntity site);

    //удалить страницу по id
    void delById(int id);

    //удалить все
    void clear();

    //отфильтровать список page_id по заданному сайту (те убрать страницы не с указанного сайта)
    List<Integer> filterPageIdListBySite(List<Integer> pageIdList, String url);

    //получить страницу по id
    PageEntity getPage(int id);

    //получить id по адресу страницы и id сайта
    int getIdByPathAndSite(String path, SiteEntity site);

    //получить список id страниц по сайту
    List<Integer> getListIdBySite(SiteEntity site);

    //получить все ссылки с сайта по его id
    List<String> getAllLinksBySiteId(SiteEntity site);

}
