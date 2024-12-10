package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigAppl;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {
    private final PageRepository pageRep;
    private final SiteRepository siteRep;
    private ConfigAppl config;

    //кол страниц по заданному id сайта
    public int getCountBySiteId(int id){
        return pageRep.getCountBySiteId(id);
    }

    //кол записей
    public long count(){
        return pageRep.count();
    }

    //Добавить сущность
    @Override
    public PageEntity savePage(PageEntity page){
        try {
            return pageRep.save(page);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(">>>> pgSrvImpl " + page.getPath() + "/ siteId " + page.getSiteId());
            return null;
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

    //удалить все
    @Override
    public void clear(){
        pageRep.clear();
    }
}
