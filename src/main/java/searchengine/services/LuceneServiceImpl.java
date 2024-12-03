package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LuceneServiceImpl implements LuceneService {
    private final LuceneMorphology luceneMorphology;
    private final LemmaService lemmaService;
    private final IndexService indexService;

    //получить карту лемм с кол их вхождений для одной страницы
    @Override
    public HashMap<String,Integer> getLemmaMap(String content){
        HashMap<String,Integer> lemmaMap = new HashMap<>();
        try {
            List<String> tokenList = getTokenList(content);
            for (String term : tokenList) {
                //доп проверочка на приемлемость токена
                if (!checkTerm(term)){
                    continue;
                }
                //получение лемм от текущего термина
                List<String> lemmaList = getLemma(term.toLowerCase());
                for (String item : lemmaList){
                    //добавление слова в мапу и подсчет кол.
                    addLemma(lemmaMap, item);
                }
            }
        }catch (Exception e){
            return null;
        }
        return lemmaMap;
    }

    //разбить текст на токены, отфильтовать
    private List<String> getTokenList(String content) throws IOException {
        List<String> list = new ArrayList<>();

        RussianAnalyzer ra = new RussianAnalyzer();
        StandardTokenizer tokenizer = new StandardTokenizer();
        tokenizer.setReader(new StringReader(content));
        StopFilter stopFilter = new StopFilter(tokenizer, ra.getStopwordSet());
        stopFilter.reset();
        while(stopFilter.incrementToken()){
            list.add(stopFilter.getAttribute(CharTermAttribute.class).toString());
        }
        stopFilter.close();
        tokenizer.close();
        ra.close();

        return list;
    }

    //получить список "основ" из слова
    private List<String> getLemma(String word){

        if (!luceneMorphology.checkString(word)){
            return Collections.emptyList();
        }
        try{
            return luceneMorphology.getNormalForms(word);
        }catch (Exception e){
            System.out.println("stop");
            return Collections.emptyList();
            }
    }

    //добавление леммы в мапу
    private void addLemma(HashMap<String,Integer> map, String s ){
        if (map.containsKey(s)){
            map.put(s, map.get(s) + 1 );
        }else{
            map.put(s,1);
        }
    }

    //доп фильтрация
    private boolean checkTerm(String term){
        //цифры но не телфон
        if (term.length() < 3){
            return false;
        }
        //проверка- телефон или нет
        if (checkPhone(term)){
            return true;
        }
        //цифровые ноне телефон убрать
        if (StringUtils.isNumeric(term)){
            return false;
        }
        return true;
    }

    //проверка на телефон
    private boolean checkPhone(String s){
        //формат слитный 89525845224 bkb c +7
        if (s.endsWith("[0-9]{10}")){
            if (s.startsWith("+7") || s.startsWith("8")){
                return true;
            }
        }
        //формат с разделителями 913-920-54-88
        if (s.endsWith("[\\W][0-9]{3}[\\W][0-9]{3}[\\W][0-9]{2}[\\W][0-9]{2}")){
            if (s.startsWith("+7") || s.startsWith("8")) {
                return true;
            }
        }
        return false;
    }

    //выполнить поисковый запрос по всему списку сайтов
    @Override
    public boolean search(String query) throws IOException{
        List<String> lemmaList = new ArrayList<>();
        String forQuery; //список в нужном для sql виде
        //разбиение запроса на токены, фильтрация
        //получение по каждому токену леммы
        for (String term : getTokenList(query)){
            lemmaList.addAll( getLemma(term) );
        }

        if (lemmaList.isEmpty()) { return false; }

        //получить список лемм в виде пригодном для SQL
        forQuery = getSqlList(lemmaList);

        //получить список lemma_Id для лемм из запроса в порядке убывания их частоты
        List<Integer> listLemmaId = lemmaService.getIdList(forQuery);
        //forQuery = getSqlList(listLemmaId);

        //получить список page_id для страниц по этим леммам. Последовательные запросы по леммам.
        //Начиная с первой получаем page_id страниц, для каждой последующей список page_id фильтруем по текущей лемме
        List<Integer> listPageId =new ArrayList<>();
        listPageId = indexService.getIdListByLemmaId(listLemmaId.get(0));
        for (int i = 1; i < listLemmaId.size(); i++){
            forQuery = getSqlList(listPageId);
            listPageId.clear();
            listPageId = indexService.filterPageIdByLemmaId(forQuery, listLemmaId.get(i));
            if (listPageId.isEmpty()){ return false;}
        }
        //тут получен список page_id с искомыми леммами
        return true;
    }

    //выполнить поисковый запрос по одному конкретному сайту
    @Override
    public boolean search(String query, String siteUrl){
        return false;
    }

    //привести список List<> к виду списка для SQL ('a','b','c','d')
    private String getSqlList(List<?> list){
        return "('" + StringUtils.join(list, "','") + "')";
    }

}
