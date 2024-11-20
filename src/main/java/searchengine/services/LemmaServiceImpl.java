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

    //получить лемму по ее ID
    @Override
    public Optional<LemmaEntity> getById(int id) {
        try{
            return lemmaRep.findById(id);
        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
