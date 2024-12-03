package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaEntity;
import searchengine.repositories.LemmaRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService{
    private final LemmaRepository lemmaRep;

    //добавить лемму
    @Override
    public LemmaEntity saveLemma(LemmaEntity lemma) {
        try{
            return lemmaRep.save(lemma);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //удалить лемму
    @Override
    public boolean delLemma(int id) {
        try{
            lemmaRep.deleteById(id);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //удалить все леммы содержащие ID сайта
    @Override
    public boolean delBySiteId(int siteId) {
        try{
            lemmaRep.delAllBySiteId(siteId);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //удалить все леммы по url сайта
    @Override
    public void delAllBySiteUrl(String siteUrl){
        lemmaRep.delAllBySiteUrl(siteUrl);
    }

    //изменить лемму
    @Override
    public boolean updateLemma(LemmaEntity lemma) {
        try{
            lemmaRep.save(lemma);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //получить лемму по ее ID и вернуть Optional
    @Override
    public Optional<LemmaEntity> getOptnlById(int id) {
        return lemmaRep.findById(id);
    }

    //получить лемму по ее ID и вернуть Entity
    @Override
    public LemmaEntity getEntityById(int id){
        return lemmaRep.getEntityById(id);
    }

    //получить лемму по самому слову
    public LemmaEntity getEntityByLemma(String word){
        return lemmaRep.getEntityByLemma(word);
    }

//    //получить frequency по имени (лемме)
//    public int getCountByName(String name){
//        lemmaRep.
//    }

    //кол лемм по указанному id сайта
    @Override
    public int getCountBySiteId(int id){
        return lemmaRep.getCountBySiteId(id);
    }

    //удалить все леммы по ID сайта
    @Override
    public void delAllBySiteId(int id){
        lemmaRep.delAllBySiteId(id);
    }

    //удалить все
    @Override
    public void clear(){
        lemmaRep.clear();
    }

    //найти id лемм из запроса и выстроить по убыванию частоты
    @Override
    public List<Integer> getIdList(String listLemma){
        return lemmaRep.getIdList(listLemma);
    }

    @Override
    public List<Integer> test(String query){
        return lemmaRep.getIdList(query);
    }

}
