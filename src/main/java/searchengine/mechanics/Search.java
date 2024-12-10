package searchengine.mechanics;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import searchengine.config.ConfigAppl;
import searchengine.model.IndexEntity;
import searchengine.repositories.IndexRepository;
import searchengine.services.LuceneService;
import searchengine.services.SiteService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Search {
    private final LuceneService luceneService;
    private final JdbcTemplate jdbcTemplate;
    private final ConfigAppl configAppl;

    //выполнить поисковый запрос
    public boolean search(String query, String siteUrl) throws IOException{
        List<IndexEntity> listPageId = getSuitableIndex(query, siteUrl);
        //*todo получен удовлетворябщий список page_id
        //*нужно посчитать ранги



        return false;
    }

    //Получить из запроса леммы слов. Получить список их id по убыванию частоты.
    //Получить список page_id подходящих страниц для первой леммы.
    //Отфильтровать их в зависимости от того указан ли конкретный сайт или нет.
    //Фильтровать их далее добавляя к условиям поочередно леммы.
    private List<IndexEntity>  getSuitableIndex(String query, String siteUrl) throws IOException{
        List<String> lemmaList = new ArrayList<>();
        List<Integer> listLemmaId = new ArrayList<>();
        List<IndexEntity> indexList = new ArrayList<>();//список походящих индекстов
        String sqlQuery, sqlSubQuery, sqlSubSubQuery;


        //полоучить список из лемм одного термина (могут быть фононимы, поэтому берем первый элемент)
        //todo фононимыбы отключить бы/ Но не знаю как
        for (String term : luceneService.getTokenList(query)){
            lemmaList.add( luceneService.getLemma(term).get(0) );
        }

        //получить список лемм отсортироанных по частоте (лемма именно слово, не id)
        //и без часто встречающихся слов (maxFrequency)
        sqlSubQuery = "( SELECT lemma.lemma, SUM(frequency) " +
                    "FROM lemma WHERE lemma IN " + getSqlList(lemmaList) + " " +
                    "GROUP BY lemma.lemma " +
                    "HAVING SUM(frequency)< " + configAppl.getMaxFrequency() + " " +
                    "ORDER BY SUM DESC) ";
        sqlQuery = "SELECT lemma FROM " + sqlSubQuery + "ORDER BY SUM DESC";
        lemmaList = jdbcTemplate.queryForList(sqlQuery, String.class);

        if (lemmaList.isEmpty()) { return indexList; } //нет лемм => нет результата

        //получить page_id по самую распространенной лемме и отфильтровать
        //в соотв с заданным сайтом поиска. (или не заданным)
        List<Integer> listPageId;
        sqlSubQuery = " ( SELECT id FROM lemma WHERE lemma.lemma = '" + lemmaList.get(0) + "' ) ";
        sqlQuery = "SELECT page_id FROM search_index WHERE lemma_id IN " + sqlSubQuery + ";";
        listPageId = jdbcTemplate.queryForList(sqlQuery, Integer.class);
        listPageId = filterPageId(listPageId, siteUrl);

        //фильтровать последовательно добаляя по одной лемме
        for (int i = 1; i < lemmaList.size(); i++){
            sqlSubSubQuery = " ( SELECT * FROM search_index WHERE page_id IN " + getSqlList(listPageId) + " ) ";  //список инднксов по списку page_id
            sqlSubQuery = " ( SELECT id FROM lemma WHERE lemma.lemma = '" + lemmaList.get(i) + "' ) ";  //список lemma_id по самому слову-лемме
            sqlQuery = " SELECT page_id FROM " + sqlSubSubQuery + " WHERE lemma_id IN " + sqlSubQuery + " ;";
            //listPageId.clear();
            listPageId = jdbcTemplate.queryForList(sqlQuery, Integer.class);
            if (listPageId.isEmpty()){ return indexList;} //нет страниц удовлетворяющих условию
        }
        //тут получен список page_id с искомыми леммами
        sqlSubQuery = "(SELECT * FROM search_index WHERE page_id IN " + getSqlList(listPageId) + ")";
        sqlQuery = "SELECT * FROM " + sqlSubQuery + " WHERE lemma_id IN " + getSqlList(listLemmaId) + ";";
        indexList = jdbcTemplate.queryForList(sqlQuery, IndexEntity.class);
        return indexList;
    }

    //отфильтровать список page_id в соответствии с выбранным сайтом (или не выбранным)
    private List<Integer> filterPageId(List<Integer> listIn, String url){
        if (url.isEmpty()){ return listIn; }
        Integer siteId = jdbcTemplate.queryForObject("SELECT id FROM site WHERE url = " + url + ";", Integer.class);
        String sqlQuery = "SELECT id FROM (SELECT * FROM page WHERE id IN " + getSqlList(listIn) + ") " +
                            " WHERE site_id = " + siteId + ";";
        return jdbcTemplate.queryForList(sqlQuery, Integer.class);
    }

    //привести список List<> к виду списка для SQL ('a','b','c','d')
    private String getSqlList(List<?> list){
        return "('" + StringUtils.join(list, "','") + "')";
    }

    //получить ранги
    private void grtRankLemma(List<Integer> listPageId, List<Integer> listLemmaId){
        //* todo не написано еще
    }

}
