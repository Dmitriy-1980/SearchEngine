package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {
    private final SiteRepository siteRep; //инжекция через конструктор
    private final JdbcTemplate jdbcTemplate;



    @Override
    public void saveSite(SiteEntity site){
        try{
            siteRep.save(site);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //найти кол записей
    public long count(){
        return siteRep.count();
    }

    //получить сайт по его URL и вернуть Entity
    @Override
    public SiteEntity findByUrl(String url) {
        return siteRep.findByUrl(url);
    }

    //проверить наличие сайтов у которых индексация еще идет
    public boolean existIndexing(){
        return siteRep.existIndexing();
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
