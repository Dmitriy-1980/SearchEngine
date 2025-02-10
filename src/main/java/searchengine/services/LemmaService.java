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


    //изменение frequency на заданную величину.
    LemmaEntity changeFrequency(int lemmaId, int addValue);

    //декремент frequency по списку id
    void frequencyDecrement(List<Integer> listId);

    //получить список лемм отсортированный по количеству страниц имеющих лемму
    List<String> getLemmaListSortedByPagesCount(List<String> lemmas);

    //получить список id по лемме (одно слово может несколько раз быть- на разных страницах)
    List<Integer> getListIdByLemma(String word);

    //получить лемму по siteId и lemma
    LemmaEntity getBySiteIdAndLemma(int siteId, String lemma);

    //получить все леммы сайта по его id
    List<LemmaEntity> getAllLemmasBySiteId(int siteId);
}
