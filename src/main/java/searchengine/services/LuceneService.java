package searchengine.services;

import java.io.IOException;
import java.util.HashMap;

public interface LuceneService {

    //получить карту лемм с кол их вхождений
    HashMap<String,Integer> getLemmaMap(String content);

    //выполнить поисковый запрос по всем сайтам спимка индексации
    boolean search(String query) throws IOException;

    //выполнить поисковый запрос
    boolean search(String query, String siteUrl ) throws IOException;
}
