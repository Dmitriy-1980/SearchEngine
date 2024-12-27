package searchengine.services;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigAppl;
import searchengine.model.LemmaEntity;
import searchengine.model.QLemmaEntity;
import searchengine.model.QSiteEntity;
import searchengine.repositories.LemmaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LemmaServiceImpl implements LemmaService{
    private final LemmaRepository lemmaRep;
    @PersistenceContext
    private final EntityManager entityManager;
    private final SiteService siteService;
    private final ConfigAppl configAppl;

    //добавить лемму
    @Override
    public LemmaEntity saveLemma(LemmaEntity lemma) {
        return lemmaRep.save(lemma);
    }

    //удалить все леммы по url сайта
    @Override
    public void delAllBySiteUrl(String siteUrl){
        //lemmaRep.delAllBySiteUrl(siteUrl);
        QLemmaEntity qLemma = QLemmaEntity.lemmaEntity;
        QSiteEntity qSite = QSiteEntity.siteEntity;
        JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
        int siteId = jqf.select(qSite.id).from(qSite).where(qSite.url.eq(siteUrl)).fetchOne();
        jqf.delete(qLemma).where(qLemma.siteId.eq(siteId));
    }

    //кол лемм по указанному id сайта
    public int getCountBySiteId(int id){
        //return lemmaRep.getCountBySiteId(id);
        JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
        QLemmaEntity qLemma = QLemmaEntity.lemmaEntity;
        return  (int) jqf.selectFrom(qLemma).where(qLemma.siteId.eq(id)).stream().count();
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

    //обновление записи по самой лемме (тк леммы для разных сайтов могут пересекаться, то нужна привязка к сайту)
    @Override
    public LemmaEntity update(int siteId, String lemma, int count){
        //List<LemmaEntity> list = lemmaRep.getEntityByLemma(lemma);
        JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
        QLemmaEntity qLemma = QLemmaEntity.lemmaEntity;
        LemmaEntity lemmaEntity = jqf.selectFrom(qLemma).where(qLemma.lemma.eq(lemma)).where(qLemma.siteId.eq(siteId)).fetchOne();
        lemmaEntity.setFrequency((float)count);
        return lemmaRep.save(lemmaEntity);
    }

    //получить список лемм отсортированный по количеству страниц имеющих лемму
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
        //Predicate byMaxCount = cb.gt(root.get("count"), configAppl.getMaxFrequency() ); вместо рпедиката по агрФункции (котне раб) нужно having !
        cq.select(root.get("lemma"))
                .where(inList)
                .groupBy(root.get("lemma"))
                .having(cb.lt(cb.count(root), configAppl.getMaxFrequency() ))
                .orderBy(cb.desc(count));
        return entityManager.createQuery(cq).getResultList();
    }

    //получить список id по лемме (одно слово может несколько раз быть- на разных страницах)
    public List<Integer> getListIdByLemma(String word){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
        Root<LemmaEntity> root = cq.from(LemmaEntity.class);
        cq.select(root.get("id")).where(cb.equal(root.get("lemma"), word));
        return entityManager.createQuery(cq).getResultList();
    }

}
