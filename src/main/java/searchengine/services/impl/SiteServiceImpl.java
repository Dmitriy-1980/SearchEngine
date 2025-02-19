package searchengine.services.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexingStatus;
import searchengine.model.QSiteEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteService;

@Service
@RequiredArgsConstructor
@Transactional
public class SiteServiceImpl implements SiteService {
    private final SiteRepository siteRep; //инжекция через конструктор
    @PersistenceContext
    private final EntityManager entityManager;



    @Override
    public SiteEntity saveSite(SiteEntity site){
        return siteRep.save(site);
    }

    //найти кол записей
    public long count(){
        return siteRep.count();
    }

    //получить сайт по его URL и вернуть Entity
    @Override
    public SiteEntity findByUrl(String url) {
        JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
        QSiteEntity qSite = QSiteEntity.siteEntity;
        return jqf.selectFrom(qSite).where(qSite.url.eq(url)).fetchOne();
    }

    //проверить наличие сайтов у которых индексация еще идет
    public boolean existIndexing(){
        JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
        QSiteEntity qSite = QSiteEntity.siteEntity;
        long count = jqf.selectFrom(qSite).where(qSite.status.eq(IndexingStatus.INDEXING.toString())).fetch().stream().count();
        return (count > 0);
    }

    //удалить сайт по его ID
    @Override
    public void delById(int id) {
        siteRep.deleteById(id);
    }

    //проверить наличие сайта по url
    @Override
    public boolean existUrl(String url){
        JPAQueryFactory jqf = new JPAQueryFactory(entityManager);
        QSiteEntity qSite = QSiteEntity.siteEntity;
        long count = jqf.selectFrom(qSite).where(qSite.url.eq(url)).stream().count();
        return (count > 0);
    }

    //удалить все
    @Override
    public void clear(){
        siteRep.deleteAll();
    }
}
