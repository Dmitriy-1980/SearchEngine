package searchengine.mechanics;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.config.Config;
import searchengine.config.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexServiceImpl;
import searchengine.services.LemmaServiceImpl;
import searchengine.services.PageServiceImpl;
import searchengine.services.SiteServiceImpl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@RequiredArgsConstructor
public class Indexing {
    private final Config config;

    private final SiteServiceImpl siteService;
    private final PageServiceImpl pageService;
    private final LemmaServiceImpl lemmaService;
    private final IndexServiceImpl indexService;

    private final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    private Boolean isRunning = false;




    //запустить индексацию по списку из конфигурации в application.yml
    public boolean startFromList(){
        long start = System.currentTimeMillis();
        if (notMayStart()){
            return false;
        }
        System.out.println("Индексация запускается. " + LocalDateTime.now());
        config.checkDuplicate();
        for (Site site : config.getSites()){
            goIndex(site);//запуск индексации очередного сайта
        }
        waitOfIndexingEnd();//ожидание окончания индексации
        System.out.println("Индексация закончена " + LocalDateTime.now() + " - " +
                (System.currentTimeMillis() - start));
        isRunning = false;
        return true;
    }

    //запуск индексации дополнительного сайта
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
        clearSiteData( site.getUrl() );
        PageParser pageParser = new PageParser(site.getUrl(), linksSet, pool,
                siteService, pageService, lemmaService , indexService,
                1, null, config);
        pool.submit(pageParser);
    }

    //ожидание окончания индексации (в случае единственного пула)
    private void waitOfIndexingEnd(){
        while (true){
            if (pool.isQuiescent() && pool.getQueuedTaskCount()==0){
                return;
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
        pool.shutdown();
        //ожидаение завершения всех пулов.
        while (true){
            if (pool.isShutdown()){
                return true;
            }
        }
    }

    //удалить все данные указанного сайта
    private void clearSiteData(String url){
        if (siteService.existUrl(url)){
            int siteId = siteService.getEntityByUrl(url).getId();
            pageService.delAllBySiteId(siteId);
            siteService.delById(siteId);
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
