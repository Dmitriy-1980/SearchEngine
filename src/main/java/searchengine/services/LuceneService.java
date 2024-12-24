package searchengine.services;

import java.util.HashMap;
import java.util.List;

public interface LuceneService {

    //получить карту лемм с кол их вхождений
    HashMap<String,Integer> getLemmaMap(String content);

    //получить список "основ" из слова
    List<String> getLemma(String word);

    //получить список уникальных лемм из текста
    List<String> getUniqLemmaList(String text);
}
