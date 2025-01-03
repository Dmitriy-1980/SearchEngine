package searchengine.services;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigAppl;
import searchengine.model.IndexEntity;
import searchengine.model.QSiteEntity;
import searchengine.repositories.IndexRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class IndexServiceImpl implements IndexService{

    private final IndexRepository indexRep;
    @PersistenceContext
    private final EntityManager entityManager;
    private final LemmaService lemmaService;
    private final SiteService siteService;
    private final ConfigAppl configAppl;

    //сохранить индекс
    @Override
    public void saveIndex(IndexEntity indexEntity){
        indexRep.save(indexEntity);
    }

    //удалить всех по Url сайта
    @Override
    public void delAllBySiteUrl(String siteUrl){
        //indexRep.delAllBySiteUrl(siteUrl);
        QSiteEntity qSite = QSiteEntity.siteEntity;
        JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
        jqf.delete(qSite).where(qSite.url.eq(siteUrl));
    }

    //удалить все
    @Override
    public void clear(){
        //indexRep.clear();
        indexRep.deleteAll();
    }

    //получить список id страниц по конкретной лемме
    @Override
    public List<Integer> getPageIdListByLemma(String word){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
        Root<IndexEntity> root = cq.from(IndexEntity.class);

        CriteriaBuilder.In<Integer> inList = cb.in(root.get("lemmaId"));
        lemmaService.getListIdByLemma(word).forEach(id -> inList.value(id));

        cq.select(root.get("pageId")).where(inList);
        return entityManager.createQuery(cq).getResultList();
    }

    //убрать из списка id страниц те, на которых заданная лемма не появляется
    @Override
    public List<Integer> filterPageIdListByLemmaId(List<Integer> pageIdList, String word){
        //получить лемма_id, и фильтровать индексы- изсписка page_id убрать те, в которые lemma_id не попадает
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
        Root<IndexEntity> root = cq.from(IndexEntity.class);

        //создать список предикатов по условию lemma_id из переданного списка
        CriteriaBuilder.In<Integer> inListPageId = cb.in(root.get("pageId"));
        pageIdList.forEach(id -> inListPageId.value(id));

        //создать список предикатов по условию lemma_id из заданного списка
        List<Integer> listLemmaId = lemmaService.getListIdByLemma(word);
        CriteriaBuilder.In<Integer> inListLemmaId = cb.in(root.get("lemmaId"));
        listLemmaId.forEach(id -> inListLemmaId.value(id));

        cq.select(root.get("pageId"))
                .where(inListLemmaId, inListPageId);

        return entityManager.createQuery(cq).getResultList();
    }

    //получить сумму rank всех лемм по списку
    @Override
    public Float getSummaryRank(List<Integer> lemmaIdList){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Float> cq = cb.createQuery(Float.class);
        Root<IndexEntity> root = cq.from(IndexEntity.class);
        CriteriaBuilder.In<Integer> inList = cb.in(root.get("lemmaId"));
        lemmaIdList.forEach(id -> inList.value(id));
        cq.select(cb.sum(root.get("rank"))).where(inList);
        return entityManager.createQuery(cq).getSingleResult();
    }

    //получить все индексы относящиеся к заданной странице
    @Override
    public List<Integer> getAllLemmaIdByPageId(int pageId){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
        Root<IndexEntity> root = cq.from(IndexEntity.class);
        cq.select(root.get("lemmaId")).where(cb.equal(root.get("pageId"), pageId));
        return entityManager.createQuery(cq).getResultList();
    }

}
