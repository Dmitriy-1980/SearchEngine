package searchengine.services;

import searchengine.model.IndexEntity;

public interface IndexService {

    //сохранить индекс
    void saveIndex(IndexEntity indexEntity);

    //удалить всех по Url сайта
    void delAllBySiteUrl(String siteUrl);

    //удалить все
    void clear();

}
