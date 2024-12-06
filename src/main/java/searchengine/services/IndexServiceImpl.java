package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.repositories.IndexRepository;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService{

    private final IndexRepository indexRep;

    //сохранить индекс
    @Override
    public void saveIndex(IndexEntity indexEntity){
        indexRep.save(indexEntity);
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

}
