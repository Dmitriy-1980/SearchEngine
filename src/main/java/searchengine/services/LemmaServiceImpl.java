package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaEntity;
import searchengine.repositories.LemmaRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService{
    private final LemmaRepository lemmaRep;

    //добавить лемму
    @Override
    public boolean addLemma(LemmaEntity lemma) {
        try{
            lemmaRep.save(lemma);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
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

    //кол лемм по указанному id сайта
    @Override
    public int getCountBySiteId(int id){
        return lemmaRep.getCountBySiteId(id);
    }

    //удалить все леммы по ID сайта
    public void delAllBySiteId(int id){
        lemmaRep.delAllBySiteId(id);
    }


}
