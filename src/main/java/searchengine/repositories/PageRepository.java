package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    //получить список страниц по заданному сайту
    List<PageEntity> getAllBySiteId(SiteEntity site);

    //удалить страницы по заджанному сайту
    void deleteAllBySiteId(SiteEntity site);

    //получить id страница по ее адресу и id сайта
    //Тут в струтуре БД исторически siteId представлен сущностью сайта. Поэтому поле SiteEntity
    PageEntity getByPathAndSiteId(String path, SiteEntity siteId);

    //найти страницу по url
    PageEntity getByPath(String path);
}
