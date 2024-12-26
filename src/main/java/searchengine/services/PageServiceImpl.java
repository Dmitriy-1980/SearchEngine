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
import searchengine.model.PageEntity;
import searchengine.model.QPageEntity;
import searchengine.repositories.PageRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PageServiceImpl implements PageService {
    private final PageRepository pageRep;
    @PersistenceContext
    private final EntityManager entityManager;
    private final ConfigAppl configAppl;
    private final SiteService siteService;

    //кол страниц по заданному siteId сайта
    public int getCountBySiteId(int siteId){
         JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
        QPageEntity qPage = QPageEntity.pageEntity;
        return (int) jqf.selectFrom(qPage).where(qPage.siteId.id.eq(siteId)).stream().count();
    }

    //кол записей
    public long count(){
        return pageRep.count();
    }

    //Добавить сущность
    @Override
    public PageEntity savePage(PageEntity page){
        return pageRep.save(page);
    }

    //удалить все стриницы по ID сайта
    @Override
    public void delAllBySiteId(int siteId) {
       JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
       QPageEntity qPage = QPageEntity.pageEntity;
       jqf.delete(qPage).where(qPage.siteId.id.eq(siteId));
    }

    //удалить все
    @Override
    public void clear(){
          pageRep.deleteAll();
    }

    //отфильтровать список page_id по заданному сайту (те убрать страницы не с указанного сайта)
    @Override
    public List<Integer> filterPageIdListBySite(List<Integer> pageIdList, String url){
        if (url == null || url.isEmpty()
                || !configAppl.isExistsUrl(url)
                || !siteService.existUrl(url)){
            return pageIdList;
        }
        int siteId = siteService.findByUrl(url).getId();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
        Root<PageEntity> root = cq.from(PageEntity.class);

        CriteriaBuilder.In<Integer> inList = cb.in(root.get("id"));
        pageIdList.forEach(id -> inList.value(id));

        cq.select(root.get("id"))
                .where(inList, cb.equal(root.get("siteId"), siteId));

        return entityManager.createQuery(cq).getResultList();
    }


    //получить страницу по id
    @Override
    public PageEntity getPage(int id){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PageEntity> cq = cb.createQuery(PageEntity.class);
        Root<PageEntity> root = cq.from(PageEntity.class);
        cq.where(cb.equal(root.get("id"), id));

        return entityManager.createQuery(cq).getSingleResult();
    }
}
