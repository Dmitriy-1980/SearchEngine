package searchengine.services;

import searchengine.model.LemmaEntity;

import java.util.List;

public interface LemmaService {

    //добавить лемму
    LemmaEntity saveLemma(LemmaEntity lemma);

    //удалить все леммы по url сайта
    void delAllBySiteUrl(String siteUrl);

    //кол лемм по указанному id сайта
    int getCountBySiteId(int id);

    //кол записей
    long count();

    //удалить все
    void clear();

    //обновление записи по самой лемме (тк леммы для разных сайтов могут пересекаться, то нужна привязка к сайту)
    LemmaEntity update(int siteId, String lemma, int count);

    //получить список лемм отсортированный по количеству страниц имеющих лемму
    List<String> getLemmaListSortedByPagesCount(List<String> lemmas);

    //получить список id по лемме (одно слово может несколько раз быть- на разных страницах)
    List<Integer> getListIdByLemma(String word);

//    //получить сумму rank всех лемм по списку
//    Float getSummaryRank(List<Integer> lemmaIdList);
}
