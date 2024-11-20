package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {
    private final SiteRepository siteRep; //инжекция через конструктор
    private final JdbcTemplate jdbcTemplate;


    //добавить сайт по его
    @Override
    public boolean addSite(String url, String name) {
        SiteEntity site = new SiteEntity();
        site.setName(name);
        site.setStatus(IndexingStatus.INDEXING.toString());
        site.setStatusTime(LocalDateTime.now());
        site.setUrl(url);
        try {
            siteRep.save(site);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //получить сайт по его URL
    @Override
    public Optional<SiteEntity> getByUrl(String url) {
        SiteEntity site = (SiteEntity) siteRep.findByUrl(url);
        if (site == null){
            return Optional.empty();
        }else {
            return Optional.of(site);
        }
    }

    //получить сайт по его id
    @Override
    public Optional<SiteEntity> getById(int id) {
        return siteRep.findById(id);
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

    //выполнить переданный запрос
//    @Override
//    public ResponseEntity<> executeQuery(String query) {
//        ResponseEntity response;
//        try{
//            return new ResponseEntity<>(siteRep.exequteQuery(query) , HttpStatus.OK);
//        } catch (Exception e){
//            e.printStackTrace();
//            return new ResponseEntity<>("ошибка поиска. " , HttpStatus.valueOf(520))
//        }
//    }

    //преобразование "сайта со статусом типа VARCHAR" в "сайт со статусом типа IbdexingStatus"
//    Site getSite(ResultSet rs){
//        Site site = new Site();
//        try{
//            site.setId( rs.getInt("id") );
//            String status = rs.getString("status");
//            if (status.equals("INDEXING")){
//                site.setStatus(IndexingStatus.INDEXING);
//            } else if (status.equals("INDEXED")) {
//                site.setStatus(IndexingStatus.INDEXED);
//            }else {
//                site.setStatus(IndexingStatus.FAILED);
//            }
//            LocalDateTime localDateTime = rs.getTime("status_time").toLocalTime().
//                    atDate( rs.getDate("status_time").toLocalDate() );
//            site.setStatusTime( localDateTime );
//            site.setLastError( rs.getString("last_error") );
//            site.setUrl( rs.getString("url") );
//            site.setName( rs.getString("name") );
//            return site;
//        }catch (SQLException e){
//            return null;
//        }
//    }


}
