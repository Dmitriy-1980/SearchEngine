package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.exceptions.RunError;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SiteCrudServiceImpl implements SiteCRUDService{
    private final SiteRepository siteRep; //инжекция через конструктор
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<Integer> addSite(String url, String name) {
        Site site = new Site();
        site = new Site();
        site.setName(name);
        site.setStatus(IndexingStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site.setUrl(url);
        Optional answer = Optional.of( siteRep.save(site) );
        if (answer.isEmpty()){
            return answer;
        } else {
            return Optional.of ( ((Site) answer.get()).getId() );
        }
    }

    @Override
    public Optional<Site> getByUrl(String url) {
        List<Site> list = siteRep.findByUrl(url);
        int length = list.size();
        if (length == 0){
            return Optional.empty();
        }else if (length == 1){
            return Optional.of(list.get(0));
        }else {
            try {
                throw new RuntimeException("По указанному url найдено более одного сайте. В работу взят первый.");
            } catch (RunError e ){
                return Optional.of(list.get(0));
            }
        }
    }

    @Override
    public Optional<Site> getById(int id) {
        List<Site> list = siteRep.findById(id);
        int length = list.size();
        if (length == 0){
            return Optional.empty();
        } else {
            return Optional.of(list.get(0));
        }
    }

    @Override
    public boolean updateByUrl(String url, Site site) {
        //если нет такого url то исключение будет при запросе к репозиторию
        Optional<Site> answer = this.getByUrl(url);
        if (answer.isEmpty()){
            return false;
        }
        int id = answer.get().getId();
        site.setId(id);
        siteRep.save(site);
        return true;
    }

    @Override
    public boolean updateById(int id, Site site) {
        //id тут и не нужен как бы. но ....
        site.setId(id);//на случай несоответствия id заданного и id в объекте
        siteRep.save(site);
        return true;
    }

    @Override
    public boolean delByUrl(String url) {
        //если нет такого url то исключение будет при запросе к репозиторию
        Optional<Site> answer = this.getByUrl(url);
        if (answer.isEmpty()){
            return false;
        }
        int id = answer.get().getId();
        siteRep.deleteById(id);
        return true;
    }

    @Override
    public boolean delById(int id) {
        siteRep.deleteById(id);
        return true;
    }
}
