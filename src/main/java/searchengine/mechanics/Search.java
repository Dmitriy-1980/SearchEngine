package searchengine.mechanics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigAppl;
import searchengine.dto.PageInResponse;
import searchengine.dto.SearchResult;
import searchengine.exceptions.SearchResultError;
import searchengine.model.PageEntity;
import searchengine.services.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private final MyLog log = new MyLog();
    @PersistenceContext
    private final EntityManager entityManager;
    private final Indexing indexing;

       /**Возвращает объект SearchResult.
     * @param "String query- строка запроса полученная отконтроллера.
     * @param "String siteUrl- адрес сайта по которому поиск. Если пуст то по всем.
     * @param "int offset- "отступ" от начала списка подходящихстраниц.
     * @param "int limit- кол. страниц в результирующем списке.
     * @return SearchResult.*/
    public SearchResult search(String query, String siteUrl, int offset, int limit) throws IOException{
        log.searchLog("search: " + query, "info");

        //тут серия проверок
        checkQueryOnError(query, siteUrl, offset, limit);

        //получить список лемм отсортированный в порядке возрастания кол страниц с ними
        //отсеивая слишко мчасто встречающиеся (где кол > предельного ConfigAppl.maxFrequency )
        List<String> sortedLemmasList =
                lemmaService.getLemmaListSortedByPagesCount(luceneService.getUniqLemmaList(query));
        if (sortedLemmasList.isEmpty())
            { throw new SearchResultError("Заданные слова в месте не встречаются.");} //нет лемм => нет результата

        //получить список id подходящих страниц
        List<Integer> listPageId = getSuitablePageIdList(siteUrl, sortedLemmasList);
        if (listPageId.isEmpty())
            { throw new SearchResultError("Заданные слова в месте не встречаются."); }

        //получить список объектов- результатов поиска
        List<PageInResponse> pagesInResponse = getPagesInResponse(listPageId, sortedLemmasList);
        //и обрезать по заданный offset и limit
        List<PageInResponse> listResult = getLimitResult(pagesInResponse, offset, limit);

        //формирование ответа
        SearchResult result = new SearchResult();
        result.setResult(true);
        result.setCount( listResult.size() );
        result.setData( listResult );

        return result;
    }



     /**Получение списка id страниц подходящих к запросу.
     * @param siteUrl сайт для поиска. (если нет то по всемищем).
     * @param lemmaList список лемм-слов из запроса.
     * @return "List<Integer>- списоу id-страниц соответствующих запросу.*/
    private List<Integer> getSuitablePageIdList(String siteUrl, List<String> lemmaList) throws IOException{
        log.searchLog("before getSuitablePageIdList()", "info");
        List<Integer> listPageId = new ArrayList<>();//список id страниц с заданными леммами

        //получить список page_id для самой редкой леммы
        listPageId = indexService.getPageIdListByLemma( lemmaList.get(0) );

        //отфильтровать список page_id в соответствии с заданным для поиска сайтом
        listPageId = pageService.filterPageIdListBySite(listPageId, siteUrl);

        //фильтровать последовательно добавляя по одной лемме
        for (int i = 1; i < lemmaList.size(); i++){
            listPageId = indexService.filterPageIdListByLemmaId(listPageId, lemmaList.get(i));
            if (listPageId.isEmpty()) { return listPageId; }
        }

        log.searchLog("listPageId: " + listPageId.toString(), "info");

        return listPageId;
    }


    /**Получение списка возвращаемых по запросу /search объектов.
     * @param listPageId список id страиц содержащих все слова из запроса.
     * @param lemmaList список лемм-слов из запроса.
     * @return "List<PageOnRequest>- список объектов PageOnRequest возвращаемых по запросу.*/
    private List<PageInResponse> getPagesInResponse(List<Integer> listPageId, List<String> lemmaList){
        log.searchLog("before getPagesInResponse() ", "info");
        float maxRel = 0;
        List<Integer> listLemmaId;
        List<PageInResponse> pageRelList = new ArrayList<>();

        //создать объект-результат поиска для каждой страницы
        for (int pageId : listPageId){
            //расчет абсолютной релевантности для текущей страницы
            listLemmaId = indexService.getAllLemmaIdByPageId(pageId);
            float absRel = indexService.getSummaryRank(listLemmaId);

            PageInResponse pr = new PageInResponse();
            PageEntity pageEntity = pageService.getPage(pageId);

            Document document = Jsoup.parse(pageEntity.getContent());
            Elements elements = document.select("title");
            String title = elements.get(0).text();

            pr.setUri(pageEntity.getPath());
            pr.setTitle(title);
            pr.setSnippet(getSnippet(document, lemmaList));
            pr.setRelevance(absRel);
            pr.setSite( pageEntity.getSiteId().getUrl() );
            pr.setSiteName( pageEntity.getSiteId().getName() );
            insertPageRelevance(pageRelList, pr);
            maxRel = Math.max(maxRel, absRel);

        }

        //расчет относительной релевантности и внесение данных посайту
        for (PageInResponse item : pageRelList){
            item.setRelevance( item.getRelevance()/maxRel );
        }

        return pageRelList;
    }



    /**Вставка в список результатов поиска List<PageOnRequest>
     * нового элемента в соответствии с его релевантностью.
     * @param "List<PageOnRequest>- целевой список.
     * @param "PageOnRequest- вставляемое значение.*/
    private void insertPageRelevance(List<PageInResponse> list, PageInResponse pr){
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



    /**Перебирает теги HTML-документа в определенном порядке
     * и в тексте каждого тега ищет искомые слова.
     * При нахождении тега содержащего все искомые слова поиск прекращается.
     * @param document код страницы.
     * @param lemmaList список лемм- искомых слов.
     * @return String- найденный кусок текста с выделенными искомыми словами.
     * <pre>
     *      Если совпадений в пределах одного элемента нет, то вернет заголовок страницы.
     * </pre>*/
    private String getSnippet(Document document, List<String> lemmaList){
        List<String> tagsName = new ArrayList<>(List.of("title","h1","h2","h3","p","span","div")); //":not(title,h1,h2,h3,p,span)"
        Snippet snippet = new Snippet(document.select("title").text(),0); //дефолтный сниппет, безсовпадений
        int wordsInQuery = lemmaList.size();
        //перебор тегов и поиск в них нужных слов
        for (String tagName : tagsName){
            boolean breakFlag = false;
            for (Element tag : document.body().select(tagName)){
                Snippet newSnippet = getSnippetOnTag(tag.text(), lemmaList );
                if (newSnippet.wordCount==wordsInQuery){
                    snippet.setText(newSnippet.getText());
                    snippet.setWordCount(newSnippet.getWordCount());
                    breakFlag = true;
                    break;
                }
                if (newSnippet.wordCount>snippet.wordCount){
                    snippet.setText(newSnippet.getText());
                    snippet.setWordCount(newSnippet.getWordCount());
                }
            }
            if (breakFlag) {break;}
        }
        log.searchLog("after getSnippet(). return: " + snippet.text, "info");
        return snippet.text;
}


    /**В переданном тексте выделяет слова из списка
     * и считает кол. найденных разных слов из этого списка.
     * При нахождении всех слов из списка поиск прекращается.
     * @param inputText текст в которовм выделяются слова.
     * @param lemmaList список выделяемых слов.*/
    private Snippet getSnippetOnTag(String inputText, List<String> lemmaList){
        List<String> foundLemmas = new ArrayList<>();
        List<String> originalWords = new ArrayList<>();
        List<String> text = List.of(inputText.split("[ :punct]+"));
        for (String word : text){
            List<String> lemmas = luceneService.getLemma(word);
            if (lemmas.isEmpty()) { continue; }
            String lemma = lemmas.get(0);
            if (lemmaList.contains(lemma)){
                if (!foundLemmas.contains(lemma)) { foundLemmas.add(lemma); }
                if (!originalWords.contains(word.toLowerCase())) {
                    originalWords.add(word.toLowerCase());
                    inputText = markWord(inputText, word);
                }
            }
        }
        return new Snippet(inputText, foundLemmas.size());
    }



    /**Выделение указанного слова(в) в тексте.>
     * @param insertText текст в которовм выделяются слова.
     * @param word выделяемое слово.
     * @returm String- текст с выделенными словами*/
    private String markWord(String insertText, String word){
        String textLowCase = insertText.toLowerCase();
        int pos = 0;
        int start = 0;
        int wordLength = word.length();
        StringBuilder result = new StringBuilder();
        while (true){
            pos = textLowCase.indexOf(word, start);
            if (pos == -1) {
                result.append(insertText.substring(start, insertText.length()));
                break;
            }
            if (pos > 3 && textLowCase.indexOf("<b>") == (start - 3) ){
                result.append( insertText.substring(start, pos + wordLength) );
                start = wordLength + pos;
            }else {
                result.append(insertText.substring(start, pos) + "<b>" + insertText.substring(pos, pos + wordLength) + "</b>");
                start = wordLength + pos;
            }
        }

        return result.toString();
    }



    /**Обрезает список результатов поиска
     * в соответствии с отступом и кол.выводимых записе (offset, limit)
     * @param inputList список результатов поиска
     * @param offset отступ от начала списка
     * @param limit кол. записей в результирующем списке.
     * @return List<PageOnRequest>- список результатов*/
    private List<PageInResponse> getLimitResult(List<PageInResponse> inputList, int offset, int limit){
        List<PageInResponse> outputList = new ArrayList<>();
        int listLength = inputList.size();
        int last = Math.min(offset + limit, listLength);

        if (offset >= listLength ) { return outputList; }//список исчерпан, вернуть пустой
        for (int i = offset; i < last; i++) {
            outputList.add( inputList.get(i) );
        }
        return outputList;
    }



    /**<pre>Проверка поискового запроса на предсказуемые ошибки:
     * пустой запрос, не верный сайт, идет индексация,
     * не верные параметры offset или limit.</pre>
     *@param query поисковый запрос
     * @param siteUrl адрес сайта
     * @param offset с какой по счету записи начать выдачу результатов
     * @param limit кол записей в выдаче
     * */
    private void checkQueryOnError(String query, String siteUrl, int offset, int limit){
        if (query.isEmpty())
        { throw new SearchResultError("Задан пустой поисковый запрос"); }
        if (!siteUrl.isEmpty() && !Utilites.isExistSite(siteUrl, configAppl.getSites()) )
        { throw new SearchResultError("Заданый сайт не из списка индексации."); }
        if (offset < 0 || limit < 0 )
        { throw new SearchResultError("Заданы не верные параметры запроса."); }
        if (indexing.getIsRunning())
        { throw new SearchResultError("В данный момент идет индексация.попробуйте позже."); }
    }





}


