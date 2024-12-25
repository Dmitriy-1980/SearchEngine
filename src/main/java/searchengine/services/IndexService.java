package searchengine.services;

import searchengine.model.IndexEntity;
import java.util.List;

public interface IndexService {

    //сохранить индекс
    void saveIndex(IndexEntity indexEntity);

    //удалить всех по Url сайта
    void delAllBySiteUrl(String siteUrl);

    //удалить все
    void clear();

    //получить список id страниц по конкретной лемме
    List<Integer> getPageIdListByLemma(String word);

    //убрать из списка id страниц те, на которых заданная лемма не появляется
    List<Integer> filterPageIdListByLemmaId(List<Integer> pageIdList, String word);

    //получить сумму rank всех лемм по списку
    Float getSummaryRank(List<Integer> lemmaIdList);

    //получить все леммы относящиеся к заданной странице
    List<Integer> getAllLemmaIdByPageId(int pageId);
}
