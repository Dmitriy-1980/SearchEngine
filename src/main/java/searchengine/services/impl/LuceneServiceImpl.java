package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.services.LuceneService;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LuceneServiceImpl implements LuceneService {
    private final LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};//названия частиц, для фильтрации

    //получить карту лемм с кол их вхождений для одной страницы
    /**Получает текст из которого удаляются все не кириллические слова
     * Создается HashMap<слово,кол.> где каждому слову соответствует
     * количество его появлений в тексте*/
    @Override
    public HashMap<String,Integer> getLemmaMap(String content){
        HashMap<String,Integer> lemmaMap = new HashMap<>();
        List<String> wordList = getListWord(content);

        for (String word : wordList){
            if (particlesFilter(word)){ continue; }
            String lemma = luceneMorphology.getNormalForms(word).get(0);
            addLemma(lemmaMap,lemma);
        }
        return lemmaMap;
    }

    /**<pre>Получает текст, заменяет все не кириллические символы
     *на пробелы, делит на слова по пробелам и возвращает коллекциюслов.</pre>*/
    public List<String> getListWord(String text){
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^а-я\\s]","")
                        .trim()
                        .split("\\s+"))
                        .toList();
    }

    /**<pre>Фильтрация частей речи списка слов.
     *Получает лингво-инфу о слове (в томчислечастьречи)
     *и проверяет на соответствие "запрещенным" частям речи.
     *(напр {"МЕЖД", "ПРЕДЛ", "СОЮЗ"})
     *@Param String - проверяемое слово
     *@Return true если слово из списка запрещенных частей речи.
     *</pre>*/
    private boolean particlesFilter(String word){
        for (String stopWord : particlesNames){
            List<String> morfList = luceneMorphology.getMorphInfo(word);
            for (String morfInfo : morfList){
                if (morfInfo.contains(stopWord.toUpperCase())){
                    return true;
                }
            }
        }
        return false;
    }

    /**@Param String текст для филтрации
     * @Return List<String> коллекция слов после фильтрации.
     * Извходящего текста удалются все слова "запрещенных" частей речи
     * и дубли. Возвращает коллекцию уникальных слов.*/
    @Override
    public List<String> getUniqLemmaList(String text){
        List<String> list = getListWord(text);
        List<String> rezList = new ArrayList<>();

        for (String word : list){
            if (particlesFilter(word)) { continue; }
            if (!rezList.contains(word)){
                rezList.add(luceneMorphology.getNormalForms(word).get(0));
            }
        }
        return rezList;
    }


    /**Возвращает список "основ" слова.
     *<pre> Сперва проверяет word- является ли оно набором
     * ТОЛЬКО кириллическихсимволов в нижнем регистре.
     * Если нет то вернет пустуюколлекцию.
     * Затем получает из воего анализатора список слов-основ.
     * Ex: "потерпевщий"->{"потерпеть","потерпвший"}</pre>*/
    @Override
    public List<String> getLemma(String word){
        if (!luceneMorphology.checkString(word)){
            return Collections.emptyList();
        }
        try{
            return luceneMorphology.getNormalForms(word);
        }catch (Exception e){
            return Collections.emptyList();
            }
    }

    /**<pre>Добавляет лемму(слово) в коллекцию.
     * Если слово уже есть, то увеличивает значение соотв. слову.
     * Новое заносит со значением = 1.
     * @Param HashMap<"слово","количество"> отображает количество добавленных одинаковых слов.
     * @Param String добавляемое слово.</></pre>*/
    private void addLemma(HashMap<String,Integer> map, String s ){
        if (map.containsKey(s)){
            map.put(s, map.get(s) + 1 );
        }else{
            map.put(s,1);
        }
    }

}
