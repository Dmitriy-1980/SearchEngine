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
    public boolean upate(PageEntity page) {
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

    @Override//СИЛЬНО ЗАПУИАНО. ПОКА НЕ ЗНАЮ КАК СДЕЛАТЬ
    //вообще не тут надо. Надо из контроллера в обработчик, (в механике) а оттуда сюда на сохранение обычным порядком.
    public boolean addThisPage(String pageUrl) {
//        String siteUrl = getSiteUrl(pageUrl);
//        List<Site> sites = siteRep.findIdByUrl(siteUrl);
//        if ( sites.isEmpty() ){
//            return false;
//        }
//        HashMap<String, ReadPageResult> map;
//        ExecutorService pool = new
//        ReadPage readPage = new ReadPage(
//                siteUrl, pageUrl, config.getDeepLimit(), config.getDeepLimit(), map,
//        );
        //
//        Page page = new Page();
//        page.setSiteId(sites.get(0));
//        page.setPath(pageUrl);
//        page.set
        return false;
    }

    //выделить из url адреса сайта
    private String getSiteUrl(String url){
        int div = url.indexOf("[a-z]/[a-z]");
        return url.substring(0, div);
    }
}
