package searchengine.mechanics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianAnalyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigAppl;
import searchengine.dto.PageOnRequest;
import searchengine.dto.SearchResult;
import searchengine.model.*;
import searchengine.services.*;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
@Transactional
public class Search {
    private final LuceneService luceneService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final PageService pageService;
    private final SiteService siteService;
    private final ConfigAppl configAppl;
    private final LuceneMorphology luceneMorphology;
    @PersistenceContext
    private final EntityManager entityManager;

    //выполнить поисковый запрос:
    //1 получить список ID подходящих страниц
    //2 пересчитать ранги и сформировать список объектов с даными по страницам
    //3 получить сниппеты
    //4 формировать ответ заданного формата
    public SearchResult search(String query, String siteUrl) throws IOException{

        if (query.isEmpty())
        { return onNegativeResult("Задан пустой поисковый запрос"); }
        if (!siteUrl.isEmpty() && !configAppl.isExistsUrl(siteUrl) )
        { return onNegativeResult("Заданый сайт не из списка индексации."); }

        //получить список лемм отсортированный в порядке убывания кол страниц с ними
        //отсеивая слишко мчасто встречающиеся (где кол > предельного ConfigAppl.maxFrequency )
        List<String> lemmaList = new ArrayList<>();
        lemmaList = lemmaService.getLemmaListSortedByPagesCount( luceneService.getUniqLemmaList(query) );
        if (lemmaList.isEmpty())
            { return onNegativeResult("Заданные слова в месте не встречаются.");} //нет лемм => нет результата

        //получить список id подходящихстраниц
        List<Integer> listPageId = getSuitablePageIdList(query, siteUrl, lemmaList);
        if (listPageId.isEmpty())
        { return onNegativeResult("Заданные слова в месте не встречаются."); }

        //получить список объектов- результатов поиска
        List<PageOnRequest> pagesOnRequests = getPagesOnRequest(listPageId, lemmaList);

        //формирование ответа
        SearchResult result = new SearchResult();
        result.setResult(true);
        result.setCount( pagesOnRequests.size() );
        result.setData( pagesOnRequests );

        return result;
    }




    //Получить список page_id подходящих страниц для первой леммы.
    //Отфильтровать их в зависимости от того указан ли конкретный сайт или нет.
    //Фильтровать их далее добавляя к условиям поочередно леммы.
    private List<Integer> getSuitablePageIdList(String query, String siteUrl, List<String> lemmaList) throws IOException{

        //List<String> lemmaList = new ArrayList<>();
        List<Integer> listPageId = new ArrayList<>();//список id страниц с заданными леммами
        //List<Integer> listLemmaId = new ArrayList<>();
        //List<IndexEntity> indexList = new ArrayList<>();//список походящих индекстов

        String sqlQuery, sqlSubQuery, sqlSubSubQuery;


        //получить список лемм отсортированный в порядке убывания кол страниц с ними
        //отсеивая слишко мчасто встречающиеся (где кол > предельного ConfigAppl.maxFrequency )
        //lemmaList = lemmaService.getLemmaListSortedByPagesCount( luceneService.getUniqLemmaList(query) );

        //if (lemmaList.isEmpty()) { return Collections.emptyList();} //нет лемм => нет результата


        //получить список page_id для самой редкой леммы
        listPageId = indexService.getPageIdListByLemma( lemmaList.get(0) );

        //отфильтровать список page_id в соответствии с заданным для поиска сайтом
        listPageId = pageService.filterPageIdListBySite(listPageId, siteUrl);

        //фильтровать последовательно добавляя по одной лемме
        for (int i = 1; i < lemmaList.size(); i++){
            listPageId = indexService.filterPageIdListByLemmaId(listPageId, lemmaList.get(i));
            if (listPageId.isEmpty()) { return listPageId; }
        }

        return listPageId;
    }





    //получить список объектов - результатов поиска
    private List<PageOnRequest> getPagesOnRequest(List<Integer> listPageId, List<String> lemmaList){
        float maxRel = 0;
        List<Integer> listLemmaId;
        List<PageOnRequest> pageRelList = new ArrayList<>();

        //создать объект-результат поиска для каждой страницы
        for (int pageId : listPageId){
            //расчет абсолютной релевантности для текущей страницы
            listLemmaId = indexService.getAllLemmaIdByPageId(pageId);
            float absRel = indexService.getSummaryRank(listLemmaId);

            PageOnRequest pr = new PageOnRequest();
            PageEntity pageEntity = pageService.getPage(pageId);

            Document document = Jsoup.parse(pageEntity.getContent());
            Elements elements = document.select("title");
            String title = elements.get(0).text();

            pr.setUri(pageEntity.getPath());
            pr.setTitle(title);
            //todo ищем сниппеты и вставляем в объект-результат
            //String s = "<B>штат</B>";
            pr.setSnipped(getSnippet(document, lemmaList));
            //pr.setSnipped(s);

            pr.setRelevance(absRel);
            pr.setSite( pageEntity.getSiteId().getUrl() );
            pr.setSiteName( pageEntity.getSiteId().getName() );
            insertPageRelevance(pageRelList, pr);
            maxRel = Math.max(maxRel, absRel);

        }



        //расчет относительной релевантности и внесение данных посайту
        for (PageOnRequest item : pageRelList){
            item.setRelevance( item.getRelevance()/maxRel );

        }

        return pageRelList;
    }



    //вставить объект pageRelevance в сортированный список согласно релевантности
    private void insertPageRelevance(List<PageOnRequest> list, PageOnRequest pr){
        if (list.isEmpty()){ list.add(pr); return; }

        int posForInsert = 0;
        for (int i = 0; i < list.size(); i++){
            if ( list.get(i).getRelevance()<pr.getRelevance() ){
                posForInsert = i;
                break;
            }
        }
        list.add(posForInsert,pr);
    }



    private String getSnippet(Document document, List<String> lemmaList){

  //в этом варианте не выделяется тэгомпо умолчанию. И результат не распознается фронтом как html

        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter(); //нет выделения полчему то
        //SimpleHTMLEncoder encoder = new SimpleHTMLEncoder();
        String queryString = lemmaList.toString();
        StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
        TokenStream tokenStream = standardAnalyzer.tokenStream("q", queryString);
        TokenGroup tokenGroup = new TokenGroup(tokenStream);
        return formatter.highlightTerm(document.toString(), tokenGroup);



   //этот вариант заканчивается выдачей чего то непонятного
/*
        WeightedSpanTerm[] lemma = new WeightedSpanTerm[lemmaList.size()];
        for (int i = 0; i < lemmaList.size(); i++) {
            lemma[i] = new WeightedSpanTerm(1, lemmaList.get(i));
        }
        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter();
        SimpleHTMLEncoder encoder = new SimpleHTMLEncoder();
        QueryScorer scorer = new QueryScorer(lemma);  //тут ошибка возникает
        Highlighter highlighter = new Highlighter(formatter, encoder, scorer);
        StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
        String html = document.html();
        String stringQuery = lemmaList.toString();
        TokenStream tokenStream = standardAnalyzer.tokenStream("field", stringQuery);
        //TokenStream tokenStream = standardAnalyzer.tokenStream("field", "блогер советский");
        try {
            //String tmp = "коко шанель была блогер, как артемий, и советский шпиен ";
            //return highlighter.getBestFragment(tokenStream, tmp);
            return highlighter.getBestFragment(tokenStream, html);
        }catch (IOException | InvalidTokenOffsetsException e){
            return "-";
        }
*/


    //этот вариант постоянно возвращает null
 /*
    List<BooleanClause> conditions = new ArrayList<>();
    for (String item : lemmaList){
        Term term = new Term(item);
        TermQuery termQuery = new TermQuery(term);
        BooleanClause booleanClause = new BooleanClause(termQuery, BooleanClause.Occur.MUST);
        conditions.add(booleanClause);
    }
    BooleanQuery booleanQuery = new BooleanQuery.Builder().add(conditions).build();

    QueryScorer queryScorer = new QueryScorer(booleanQuery);
    Highlighter highlighter = new Highlighter(queryScorer);
    StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
    String html = document.html();
    String stringQuery = lemmaList.toString();
    TokenStream tokenStream = standardAnalyzer.tokenStream("field", stringQuery);

    try {
        //tokenStream.reset();
        return highlighter.getBestFragment(tokenStream, html);
    }catch (IOException | InvalidTokenOffsetsException e ){
        return "--error on find snipped";
    }
*/


}





     //пустой запрос, сайт не из списка или нет результатов
    private SearchResult onNegativeResult(String msg){
        SearchResult searchResult = new SearchResult();
        searchResult.setResult(false);
        searchResult.setError(msg);
        return searchResult;
    }

    //НЕКАЯ ошибка при работе поисковика
    private SearchResult onSearchException(){
        SearchResult searchResult = new SearchResult();
        searchResult.setResult(false);
        searchResult.setError("Непредвиденная ошибка.");
        return searchResult;
    }


}


