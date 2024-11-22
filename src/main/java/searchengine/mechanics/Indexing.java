package searchengine.mechanics;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.config.Config;
import searchengine.config.Site;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@RequiredArgsConstructor
public class Indexing {
    private final Config config;
    private final SiteRepository siteRep;
    private final PageRepository pageRep;
    private final LemmaRepository lemmaRep;
    private final IndexRepository indexRep;
    private final HashMap<String,ForkJoinPool> poolList; //список запущенных пулов. Каждому сайту свой

    private Boolean isRunning = false;
    private ForkJoinPool pool;



    //запустить индексацию по списку из конфигурации в application.yml
    public boolean startFromList(){
        long start = System.currentTimeMillis();
        if (notMayStart()){
            return false;
        }
        System.out.println("Индексация запускается. " + LocalDateTime.now());
        config.checkDuplicate();
        for (Site site : config.getSites()){
            goIndex(site);//запуск индексации
//            HashSet<String> linksSet = new HashSet<>(); //коллекция ссылок сайта
//            site.setUrl( site.getUrl() );
//
//            ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
//            clearSiteData( site.getUrl() );
//            PageParser pageParser = new PageParser(site.getUrl(), linksSet, pool,
//                    siteRep, pageRep, lemmaRep , indexRep,
//                    1, null, config);
//            pool.submit(pageParser);
//            poolList.put(site.getUrl(), pool);
        }
        waitOfIndexingEnd();//ожидание окончания индексации
//        //ожидание окончания индексации
//        while (true){
//            Iterator< Map.Entry<String,ForkJoinPool> > iterator = poolList.entrySet().iterator();
//            while (iterator.hasNext()){
//                Map.Entry<String, ForkJoinPool> entry = iterator.next();
//                if (entry.getValue().getQueuedTaskCount() == 0 && entry.getValue().isQuiescent()){
//                    SiteEntity siteEntity = siteRep.findByUrl(entry.getKey());
//                    siteEntity.setStatus( IndexingStatus.INDEXED.toString() );
//                    siteRep.save(siteEntity);
//                    iterator.remove();
//                }
//            }
//            if (poolList.isEmpty()){
//                poolList.clear();
//                break;
//            }
//        }
        System.out.println("Индексация закончена " + LocalDateTime.now() + " - " +
                (System.currentTimeMillis() - start));
        isRunning = false;
        return true;
    }

    //запуск индексации дополнительног осайта
    public boolean startAdditionalIndexing(String url){
        long start = System.currentTimeMillis();
        if (notMayStart()){
            return false;
        }

        Site site = new Site();
        site.setUrl(url);

        goIndex(site);
        waitOfIndexingEnd();

        System.out.println("Индексация закончена " + LocalDateTime.now() + " - " +
                (System.currentTimeMillis() - start));
        isRunning = false;
        return true;
    }

    //индексация одного сайта изсписка
    private void goIndex(Site site){
        HashSet<String> linksSet = new HashSet<>(); //коллекция ссылок сайта
        site.setUrl( site.getUrl() );
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        clearSiteData( site.getUrl() );
        PageParser pageParser = new PageParser(site.getUrl(), linksSet, pool,
                siteRep, pageRep, lemmaRep , indexRep,
                1, null, config);
        pool.submit(pageParser);
        poolList.put(site.getUrl(), pool);
    }

    //ожидание окончания индексации
    private void waitOfIndexingEnd(){
        while (true){
            Iterator< Map.Entry<String,ForkJoinPool> > iterator = poolList.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, ForkJoinPool> entry = iterator.next();
                if (entry.getValue().getQueuedTaskCount() == 0 && entry.getValue().isQuiescent()){
                    SiteEntity siteEntity = siteRep.findByUrl(entry.getKey());
                    siteEntity.setStatus( IndexingStatus.INDEXED.toString() );
                    siteRep.save(siteEntity);
                    iterator.remove();
                }
            }
            if (poolList.isEmpty()){
                poolList.clear();
                break;
            }
        }
    }

    //остановить индексацию
    public boolean stop(){
        if (!isRunning){
            return false;
        }

        isRunning = false;
        //команда на завершение пулов
        for (Map.Entry<String,ForkJoinPool> entry : poolList.entrySet()){
            entry.getValue().shutdown();
        }
        //ожидаение завершения всех пулов.
        while (true){
            boolean ended = true;
            for (Map.Entry<String,ForkJoinPool> entry : poolList.entrySet()){
                if (!entry.getValue().isShutdown()){
                    ended = false;
                }
            }
            if (ended) {break;}
        }
        poolList.clear();
        return true;
    }

    //удалить все данные указанного сайта
    private void clearSiteData(String url){
        if (siteRep.existUrl(url)){
            int siteId = siteRep.findByUrl(url).getId();
            pageRep.delAllBySiteId( siteId );
            siteRep.deleteById( siteId );
        }
    }

    //проверка идет ли уже синхронизация
    private synchronized boolean  notMayStart(){
        if (isRunning){
            return true;
        }
        else{
            isRunning = true;
            return false;
        }
    }
}
