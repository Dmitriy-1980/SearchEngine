package searchengine.services;

import com.querydsl.jpa.EclipseLinkTemplates;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigAppl;
import searchengine.mechanics.MyLog;
import searchengine.model.LemmaEntity;
import searchengine.model.QLemmaEntity;
import searchengine.model.QSiteEntity;
import searchengine.repositories.LemmaRepository;

import java.util.List;

import static searchengine.model.QLemmaEntity.lemmaEntity;

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
        //lemmaRep.delAllBySiteUrl(siteUrl);
        QLemmaEntity qLemma = lemmaEntity;
        QSiteEntity qSite = QSiteEntity.siteEntity;
        JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
        int siteId = jqf.select(qSite.id).from(qSite).where(qSite.url.eq(siteUrl)).fetchOne();
        jqf.delete(qLemma).where(qLemma.siteId.eq(siteId));
    }

    //кол лемм по указанному id сайта
    public int getCountBySiteId(int id){
        //return lemmaRep.getCountBySiteId(id);
        JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
        QLemmaEntity qLemma = lemmaEntity;
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
        QLemmaEntity qLemma = lemmaEntity;
        try {
            LemmaEntity lemmaEntity = jqf.selectFrom(qLemma)
                    //.setLockMode(LockModeType.PESSIMISTIC_WRITE)//пессимистическая блокировка на чтение/изменение/запись
                    .where(qLemma.lemma.eq(lemma))
                    .where(qLemma.siteId.eq(siteId))
                    .fetchOne();
            lemmaEntity.setFrequency(count);
            return lemmaRep.save(lemmaEntity);
        }catch (Exception e){
            log.parsLog(e.getCause().toString(), "error");
            throw e;
        }
    }

    @Override
    @Transactional
    public LemmaEntity incrementFrequency(int siteId, String lemma){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LemmaEntity> cq = cb.createQuery(LemmaEntity.class);
        Root<LemmaEntity> rootGet = cq.from(LemmaEntity.class);

        //запрос на получение сущности
        cq.select(rootGet)
                .where(cb.equal(rootGet.get("siteId"),siteId))
                .where(cb.equal(rootGet.get("lemma"),lemma));
        LemmaEntity lemmaEntity = entityManager.createQuery(cq).getSingleResult();
        int value = lemmaEntity.getFrequency() + 1;

        CriteriaUpdate<LemmaEntity> cu = cb.createCriteriaUpdate(LemmaEntity.class);
        Root<LemmaEntity> rootUpdt = cu.from(LemmaEntity.class);
        //запрос на обновление
        cu.set(rootUpdt.get("frequency"), value)
                .where(cb.equal(rootUpdt.get("siteId"),siteId))
                .where(cb.equal(rootUpdt.get("lemma"),lemma));
        entityManager.createQuery(cu).executeUpdate();
        //обновленная сущность
        lemmaEntity = entityManager.createQuery(cq).getSingleResult();

        return lemmaEntity;

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
        //Predicate byMaxCount = cb.gt(root.get("count"), configAppl.getMaxFrequency() ); вместо рпедиката по агрФункции (котне раб) нужно having !
        cq.select(root.get("lemma"))
                .where(inList)
                .groupBy(root.get("lemma"))
                .having(cb.lt(cb.count(root), configAppl.getMaxFrequency() ))
                .orderBy(cb.asc(count));
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
