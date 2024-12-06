package searchengine.mechanics;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
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
    private final SiteService siteService;
    private final LuceneService luceneService;
    private final JdbcTemplate jdbcTemplate;
    private final IndexRepository indexRepository;

    //выполнить поисковый запрос
    public boolean search(String query, String siteUrl) throws IOException{
        List<IndexEntity> listPageId = getSuitableIndex(query, siteUrl);
        //*todo получен удовлетворябщий список page_id
        //*нужно посчитать ранги



        return false;
    }

    //Получить из зароса леммы слов. Получить список их id по убыванию частоты.
    //Получить список page_id подходящих страниц для первой леммы.
    //Отфильтровать их в зависимости от того указан ли конкретный сайт или нет.
    //Фильтровать их далее добавляя к условиям поочередно леммы.
    private List<IndexEntity>  getSuitableIndex(String query, String siteUrl) throws IOException{
        List<String> lemmaList = new ArrayList<>();
        List<Integer> listLemmaId = new ArrayList<>();
        List<IndexEntity> indexList = new ArrayList<>();//список походящих индекстов
        String sqlQuery;
        String sqlSubQuery;

        //получение из запроса токены и леммы
        for (String term : luceneService.getTokenList(query)){
            lemmaList.addAll( luceneService.getLemma(term) );
        }

        if (lemmaList.isEmpty()) { return indexList; } //нет лемм => нет результата

        //получить список lemma_id в порядке убывания их частоты
        sqlQuery = "SELECT id FROM lemma WHERE lemma IN " + getSqlList(lemmaList) + "ORDER BY frequency;";
        listLemmaId = jdbcTemplate.queryForList(sqlQuery, Integer.class);

        //получить page_id содежражих самую распространенную лемму и отфильтровать
        //в соотв с заданным сайтом поиска. (или не заданным)
        List<Integer> listPageId;
        sqlQuery = "SELECT page_id FROM search_index WHERE lemma_id = " + listLemmaId.get(0);
        listPageId = jdbcTemplate.queryForList(sqlQuery, Integer.class);
        listPageId = filterPageId(listPageId, siteUrl);

        //фильтровать последовательно добаляя по одной лемме
        for (int i = 1; i < listLemmaId.size(); i++){
            sqlSubQuery = "(SELECT * FROM search_index WHERE page_id IN " + getSqlList(listPageId) + ")";
            sqlQuery = "SELECT page_id FROM " + sqlSubQuery + "WHERE lemma_id = " + listLemmaId.get(i) + ";";
            listPageId.clear();
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
        String sqlQuery = "SELECT id FROM (SELECT * FROM page WHERE id IN " + getSqlList(listIn) +
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
