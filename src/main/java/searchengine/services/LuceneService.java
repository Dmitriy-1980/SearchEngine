package searchengine.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public interface LuceneService {

    //получить карту лемм с кол их вхождений
    HashMap<String,Integer> getLemmaMap(String content);

    //разбить текст на токены, отфильтовать
    List<String> getTokenList(String content) throws IOException;

    //получить список "основ" из слова
    List<String> getLemma(String word);

}
