package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaEntity;
import searchengine.repositories.LemmaRepository;

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

    //удалить все леммы по url сайта
    @Override
    public void delAllBySiteUrl(String siteUrl){
        lemmaRep.delAllBySiteUrl(siteUrl);
    }

    //кол лемм по указанному id сайта
    public int getCountBySiteId(int id){
        return lemmaRep.getCountBySiteId(id);
    }

    //кол записей
    public long count(){
        return lemmaRep.count();
    }

    //удалить все
    @Override
    public void clear(){
        lemmaRep.clear();
    }

    //обновление записи по самой лемме
    @Override
    public LemmaEntity update(String lemma, int count){
        LemmaEntity lemmaEntity = lemmaRep.getEntityByLemma(lemma);
        lemmaEntity.setFrequency((float)count);
        return lemmaRep.save(lemmaEntity);
    }

}
