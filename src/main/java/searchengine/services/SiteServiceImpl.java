package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {
    private final SiteRepository siteRep; //инжекция через конструктор
    private final JdbcTemplate jdbcTemplate;



    @Override
    public void addEntity(SiteEntity site){
        try{
            siteRep.save(site);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //получить сайт по его URL и вернуть Optional
    @Override
    public Optional<SiteEntity> getOptnlByUrl(String url) {
        return Optional.of(siteRep.findByUrl(url));
    }

    //получить сайт по его URL и вернуть Entity
    @Override
    public SiteEntity getEntityByUrl(String url) {
        return siteRep.findByUrl(url);
    }



    //получить сайт по его id и вернуть Optional
    @Override
    public Optional<SiteEntity> getOptnlById(int id) {
        return siteRep.findById(id);
    }

    //получить сайт по его Id и вернуть entity
    @Override
    public SiteEntity getEntityById(int id){
        return siteRep.getEntityById(id);
    }



    //обновить сайт по его url
    @Override
    public boolean updateByUrl(String url, SiteEntity site) {
        if (! siteRep.existUrl(url) ){
            return false;
        }
        site.setUrl(url);
        try{
            siteRep.save(site);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //обновить сайт по его ID
    @Override
    public boolean updateById(int id, SiteEntity site) {
        site.setId(id);//на случай несоответствия id заданного и id в объекте
        try{
            siteRep.save(site);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //удалить сайт по его URL
    @Override
    public boolean delByUrl(String url) {
        if ( ! siteRep.existUrl(url)){
            return false;
        }
        //если нет такого url то исключение будет при запросе к репозиторию
        SiteEntity site = (SiteEntity) siteRep.findByUrl(url);
        int id = site.getId();
        try{
            siteRep.deleteById(id);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //удалить сайт по его ID
    @Override
    public boolean delById(int id) {
        try{
            siteRep.deleteById(id);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //проверить наличие сайтов у которых индексация еще идет
    @Override
    public boolean existIndexing(){
        return siteRep.existIndexing();
    }

    //проверить наличие сайта по url
    @Override
    public boolean existUrl(String url){
        return siteRep.existUrl(url);
    }

    //удалить все
    @Override
    public void clear(){
        siteRep.clear();
    }
}
