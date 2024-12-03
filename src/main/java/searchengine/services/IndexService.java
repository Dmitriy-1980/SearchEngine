package searchengine.services;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;

import java.util.List;

public interface IndexService {

    //сохранить индекс
    void saveIndex(IndexEntity indexEntity);

    //удалить всех по siteId
    void delAllBySiteId(int siteId);

    //удалить всех по Url сайта
    void delAllBySiteUrl(String siteUrl);

    //удалить все
    public void clear();

    //получить список страниц по id леммы
    List<Integer> getIdListByLemmaId(int lemmaId);

    //получить список страниц (задан) с присутствием нужной леммы
    List<Integer> filterPageIdByLemmaId(String listPage, int lemmaId);


}
