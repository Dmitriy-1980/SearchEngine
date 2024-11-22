package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Config;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {
    private final PageRepository pageRep;
    private final SiteRepository siteRep;
    private Config config;


    //добавить страницу в БД
    @Override
    public Optional<PageEntity> addPage(PageEntity page) {
        try{
            return Optional.of( pageRep.save(page) );
        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    //получить страницу по ее ID
    @Override
    public Optional<PageEntity> getById(int pageId) {
        try{
            return pageRep.findById(pageId);
        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    //обновить страницу
    @Override
    public boolean update(PageEntity page) {
        try {
            pageRep.save(page);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //удалить страницу по ее ID
    @Override
    public boolean delById(int pageId) {
        try{
            pageRep.deleteById(pageId);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //удалить все стриницы по ID сайта
    @Override
    public boolean delAllBySiteId(int siteId) {
        try {
            pageRep.delAllBySiteId(siteId);
            return true;
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //кол страниц по заданному id сайта
    @Override
    public Integer getCountBySiteId(int siteId){
        return pageRep.getCountBySiteId(siteId);
    }

    //проверить наличие по path
    @Override
    public boolean existUrlWithSite(int siteId, String path ){
        return pageRep.existUrlWithSite(siteId, path);
    }


}
