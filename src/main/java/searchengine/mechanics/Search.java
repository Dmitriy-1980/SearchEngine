package searchengine.mechanics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigAppl;
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
    private final JdbcTemplate jdbcTemplate;
    private final PageService pageService;
    private final ConfigAppl configAppl;
    @PersistenceContext
    private final EntityManager entityManager;

    //выполнить поисковый запрос
    public boolean search(String query, String siteUrl) throws IOException{
        //получить список id подходящихстраниц
        List<Integer> listPageId = getSuitablePageIdList(query, siteUrl);

        //посчитать ранги страниц
        List<PageRelevance> listPageRel = getRankLemma(listPageId);


        return false;
    }

    //Получить из запроса леммы слов. Получить список их id по убыванию частоты.
    //Получить список page_id подходящих страниц для первой леммы.
    //Отфильтровать их в зависимости от того указан ли конкретный сайт или нет.
    //Фильтровать их далее добавляя к условиям поочередно леммы.
    private List<Integer> getSuitablePageIdList(String query, String siteUrl) throws IOException{

        List<String> lemmaList = new ArrayList<>();
        List<Integer> listPageId = new ArrayList<>();//список id страниц с заданными леммами
        List<Integer> listLemmaId = new ArrayList<>();
        List<IndexEntity> indexList = new ArrayList<>();//список походящих индекстов

        String sqlQuery, sqlSubQuery, sqlSubSubQuery;


        //получить список лемм отсортированный в порядке убывания кол страниц с ними
        //отсеивая слишко мчасто встречающиеся (где кол > предельного ConfigAppl.maxFrequency )
        lemmaList = lemmaService.getLemmaListSortedByPagesCount( luceneService.getUniqLemmaList(query) );

        if (lemmaList.isEmpty()) { return Collections.emptyList();} //нет лемм => нет результата


        //получить список page_id для самой распространенной леммы
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


    //получить ранги
    private List<PageRelevance> getRankLemma(List<Integer> listPageId){
        //* todo не написано еще
        float maxRel = 0;
        List<Integer> listLemmaId;
        List<PageRelevance> pageRelList = new ArrayList<>();
        //расчет абсолютной релевантности для страниц
        for (int pageId : listPageId){
            listLemmaId = indexService.getAllLemmaIdByPageId(pageId);
            float absRel = indexService.getSummaryRank(listLemmaId);
            PageRelevance pr = new PageRelevance();
            PageEntity pageEntity = pageService.getPage(pageId);

            Document document = Jsoup.parse(pageEntity.getContent());
            Elements elements = document.select("title");
            String title = elements.get(0).text();

            pr.setUri(pageEntity.getPath());
            pr.setTitle(title);
            pr.setSnipped("-");
            pr.setRelevance(absRel);
            insertPageRelevance(pageRelList, pr);
            maxRel = Math.max(maxRel, absRel);

        }
        //расчет относительной релевантности
        for (PageRelevance item : pageRelList){
            item.setRelevance( item.getRelevance()/maxRel );
        }

        return pageRelList;
    }

    //вставить объект pageRelevance в список согласно релевантности
    private void insertPageRelevance(List<PageRelevance> list, PageRelevance pr){
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

    //получить сниппеты по странице
    private List<String> getSnippet(String html, List<String> lemma){
        //Document document = Jsoup.parse(html);
        //Elements elements = document.select()
        /*todo тут пройти по document. Сперва проанализировать теги title, h1, h2, потом все остальные по порядку
            проверять текст тегов на содержание нужных лемм

         */


        return null;
    }

    //поиск сниппета по элементу html
    private String findLemmaFromQuery(List<String> list, String tagContent){
        /*todo поиск нужных слов в тексте тега,
          возвращает текст с выделением найденных слов <b></b>
         */



        return null;
    }

}
