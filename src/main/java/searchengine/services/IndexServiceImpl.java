package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.repositories.IndexRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService{

    private final IndexRepository indexRep;

    //сохранить индекс
    @Override
    public void saveIndex(IndexEntity indexEntity){
        indexRep.save(indexEntity);
    }

    //удалить всех по siteId
    @Override
    public void delAllBySiteId(int siteId){
        indexRep.delAllBySiteId(siteId);
    }

    //удалить всех по Url сайта
    @Override
    public void delAllBySiteUrl(String siteUrl){
        indexRep.delAllBySiteUrl(siteUrl);
    }

    //удалить все
    @Override
    public void clear(){
        indexRep.clear();
    }

    //получить список страниц по id леммы
    @Override
    public List<Integer> getIdListByLemmaId(int lemmaId){
        return indexRep.getIdListByLemmaId(lemmaId);
    }

    //получить список страниц (задан) с присутствием нужной леммы
    public List<Integer> filterPageIdByLemmaId(String listPage, int lemmaId){
        return indexRep.filterPageIdByLemmaId(listPage, lemmaId);
    }

}
