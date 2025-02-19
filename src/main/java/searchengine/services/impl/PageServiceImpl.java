package searchengine.services.impl;

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
import searchengine.mechanics.Utilites;
import searchengine.model.PageEntity;
import searchengine.model.QPageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.services.PageService;
import searchengine.services.SiteService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PageServiceImpl implements PageService {
    private final PageRepository pageRep;
    @PersistenceContext
    private final EntityManager entityManager;
    private final ConfigAppl configAppl;
    private final SiteService siteService;

    //наличие страницы по url
    public boolean isExistUrl(String url){
        return Optional.ofNullable(pageRep.getByPath(url)).isPresent();
    }


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

    //удалить все стриницы по сайту
    @Override
    public void delAllBySiteId(SiteEntity site) {
        System.out.println("STOP STOP STOP  pageServiseImpl.delAllBySiteId");
    }

    //удалить страницу по id
    public void delById(int id){
        pageRep.deleteById(id);
    }

    //удалить все
    @Override
    public void clear(){
        pageRep.deleteAllInBatch();
    }

    //отфильтровать список page_id по заданному сайту (те убрать страницы не с указанного сайта)
    @Override
    public List<Integer> filterPageIdListBySite(List<Integer> pageIdList, String url){
        if (url == null || url.isEmpty()
                || !Utilites.isExistSite(url, configAppl.getSites())
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
                .where(inList, cb.equal(root.get("siteId").get("id"), siteId));

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

    //получить id по адресу страницы и id сайта
    @Override
    public int getIdByPathAndSite(String path, SiteEntity site){
        return pageRep.getByPathAndSiteId(path, site).getId();
    }

    //получить список id страниц по id-сайта
    @Override
    public List<Integer> getListIdBySite(SiteEntity site){
        List<Integer> result = new ArrayList<>();
        for (PageEntity item : pageRep.getAllBySiteId(site) ){
            result.add(item.getId());
        }
        return result;
    }

    //получить все ссылки с сайта по его id
    public List<String> getAllLinksBySiteId(SiteEntity site){
        List<PageEntity> pageList = pageRep.getAllBySiteId(site);
        List<String> linkList = new ArrayList<>();
        for (PageEntity page : pageList){
            linkList.add( site.getUrl() + page.getPath());
        }
        return linkList;
    }

}
