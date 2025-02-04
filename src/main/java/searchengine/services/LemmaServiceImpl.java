package searchengine.services;

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigAppl;
import searchengine.mechanics.MyLog;
import searchengine.model.LemmaEntity;
import searchengine.repositories.LemmaRepository;

import java.util.*;


@Service
@RequiredArgsConstructor
@Transactional
public class LemmaServiceImpl implements LemmaService{
    private final LemmaRepository lemmaRep;
    @PersistenceContext
    private final EntityManager entityManager;
    private final SiteService siteService;
    private final ConfigAppl configAppl;
    private final MyLog log = new MyLog();

    //добавить лемму
    @Override
    public LemmaEntity saveLemma(LemmaEntity lemma) {
        return lemmaRep.save(lemma);
    }

    //удалить все леммы по url сайта
    @Override
    public void delAllBySiteUrl(String siteUrl){
        int siteId = siteService.findByUrl(siteUrl).getId();
        lemmaRep.deleteAllBySiteId(siteId);
    }

    //кол лемм по указанному id сайта
    public int getCountBySiteId(int id){
        return lemmaRep.countBySiteId(id);
    }

    //кол записей
    public long count(){
        return lemmaRep.count();
    }

    //удалить все
    @Override
    public void clear(){
        lemmaRep.deleteAll();
    }


    //обновление frequancy в лемме (тк леммы для разных сайтов могут пересекаться, то нужна привязка к сайту)
    @Override
    public LemmaEntity changeFrequency(int lemmaId, int addValue){
        Optional<LemmaEntity> lemmaOpt = lemmaRep.findById(lemmaId);
        if (lemmaOpt.isEmpty()) {return null;}

        LemmaEntity lemmaEntity = lemmaOpt.get();
        int newCount = Math.max(lemmaEntity.getFrequency() + addValue , 0 );
        lemmaEntity.setFrequency(newCount);
        if (newCount == 0)
        {
            lemmaRep.delete(lemmaEntity);
        }
        else {
            lemmaRep.save(lemmaEntity);
        }
        return lemmaEntity;
    }


    //инкремент frequency по списку id
    @Override
    public void frequencyDecrement(List<Integer> listId){
        StoredProcedureQuery query = entityManager.createNamedStoredProcedureQuery("frequencyDecrement");
        Integer[] mass = listId.toArray(Integer[]::new);
        //query.registerStoredProcedureParameter("list_id", Integer[].class, ParameterMode.IN);
        Integer[] mass1 = {104964, 104966, 104967};
        query.setParameter("list_id", mass1);
        query.execute();
    }


    //получить список лемм отсортированный по количеству страниц с каждой (частоте)
    //Сортировка в порядке возрастания их частоты
    //с учетом макс кол страниц с ней(отсев очень распространенных слов)
    @Override
    public List<String> getLemmaListSortedByPagesCount(List<String> lemmas){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<LemmaEntity> root = cq.from(LemmaEntity.class);
        CriteriaBuilder.In<String> inList = cb.in(root.get("lemma"));
        for (String word : lemmas){
            inList.value(word);
        }
        Expression<Long> count = cb.count(root.get("lemma"));
        cq.select(root.get("lemma"))
                .where(inList)
                .groupBy(root.get("lemma"))
                .having(cb.lt(cb.count(root), configAppl.getMaxFrequency() ))
                .orderBy(cb.asc(count));
        return entityManager.createQuery(cq).getResultList();
    }

    //получить список id по лемме (одно слово может несколько раз быть- на разных страницах)
    @Override
    public List<Integer> getListIdByLemma(String word){
        List<Integer> result = new ArrayList<>();
        for (LemmaEntity lemmaEntity : lemmaRep.findAllByLemma(word)){
            result.add(lemmaEntity.getId());
        }
        return result;
    }

    //получить лемму по siteId и lemma
    @Override
    public LemmaEntity getBySiteIdAndLemma(int siteId, String lemma){
        return lemmaRep.findBySiteIdAndLemma(siteId, lemma);
    }

    //получить все леммы сайта по его id
    public List<LemmaEntity> getAllLemmasBySiteId(int siteId){
        return lemmaRep.getBySiteId(siteId);
    }
}
