package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LuceneServiceImpl implements LuceneService {
    private final LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};//названия частиц, для фильтрации

    //получить карту лемм с кол их вхождений для одной страницы
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

    //получить список слов из текста. Получить только русские.
    private List<String> getListWord(String text){
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^а-я\\s]","")
                        .trim()
                        .split("\\s+"))
                        .toList();
    }

    //фильтрация частей речи
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

    //получить список уникальных лемм
    @Override
    public List<String> getUniqLemmaList(String text){
        List<String> list = getListWord(text);
        List<String> rezList = new ArrayList<>();

        for (String word : list){
            if (particlesFilter(word)) { continue; }
            if (!rezList.contains(word)){
                rezList.add(word);
            }
        }
        return rezList;
    }


    //получить список "основ" из слова
    @Override
    public List<String> getLemma(String word){

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

}
