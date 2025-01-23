package searchengine.mechanics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.config.ConfigAppl;
import searchengine.config.Site;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
import searchengine.services.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Getter
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@RequiredArgsConstructor
public class Indexing {
    private final ConfigAppl configAppl;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final LuceneService luceneService;
    private final MyLog log = new MyLog();

    private final ForkJoinPool pool = new ForkJoinPool();//(Runtime.getRuntime().availableProcessors());
    @Setter
    private Boolean isRunning = false;
    //список url-сайта , список задач по нему
    private final ConcurrentHashMap<String, List<RecursiveAction>> taskList = new ConcurrentHashMap<>();

    //запустить индексацию по списку из конфигурации в application.yml
    public boolean startFromList(){
        log.indLog("#### before startFromList ", "info");
        clearDB();//зачистить БД
        long start = System.currentTimeMillis();
        if (notMayStart()){
            return false;
        }
        System.out.println("Индексация запускается. " + LocalDateTime.now());
        configAppl.checkDuplicate();
        for (Site site : configAppl.getSites()){
            goIndex(site);//запуск индексации очередного сайта
        }

        //todo часть ниже нужно вынести в отдельный поток- ожидание окончания
        WaitOfIndexEnd waitOfIndexEnd = new WaitOfIndexEnd(this, start);
        pool.submit(waitOfIndexEnd);

        //waitOfIndexingEnd();//ожидание окончания индексации
        //System.out.println("Индексация закончена " + LocalDateTime.now() + " - " +
        //        (System.currentTimeMillis() - start));
        //isRunning = false;
        return true;
    }


    //запуск индексации дополнительного сайта
    public boolean startAdditionalIndexing(String url){
        log.indLog("before StartAdditionalIndexing", "info");
        long start = System.currentTimeMillis();
        if (notMayStart()){
            return false;
        }

        Site site = new Site();
        site.setUrl(url);

        clearSiteData(url);
        goIndex(site);

        //todo часть ниже нужно вынести в отдельный поток- ожидание окончания
        WaitOfIndexEnd waitOfIndexEnd = new WaitOfIndexEnd(this, start);
        pool.submit(waitOfIndexEnd);
//        waitOfIndexingEnd();
//
//        System.out.println("Индексация закончена " + LocalDateTime.now() + " - " +
//                (System.currentTimeMillis() - start));
//        isRunning = false;
        return true;
    }


    //индексация одного сайта изсписка
    private void goIndex(Site site){
        log.indLog("before goIndex", "info");
        Vector<String> linksSet = new Vector<>(); //уникальный список ссылок со всего сайта
        ConcurrentHashMap<String,Integer> siteLemmaMap = new ConcurrentHashMap<>(); //уникальный список лемм с кол их вхождений для всего сайта
        site.setUrl( site.getUrl() );
        PageParser pageParser = new PageParser(site.getUrl(), pool,
                linksSet, siteLemmaMap, taskList,
                siteService, pageService, luceneService, lemmaService , indexService,
                1, null, configAppl);
        taskList.put(site.getUrl(), new Vector<>());
        taskList.get(site.getUrl()).add(pageParser);
        pool.submit(pageParser);
    }


    //ожидание окончания индексации (в случае единственного пула)
    //И проверка статуса. Завершенные с ошибкой отметиться в записи site могут не успеть
    public void waitOfIndexingEnd(){
        log.indLog("waitOfIndexingEnd", "info");
        try {
            Thread.sleep(1000);
        }catch (InterruptedException e){
            //System.out.println("Indexing.waitOfIndexingEnd  " + e.getMessage());
            log.indLog("Interrupted.. " + e.getMessage(), "error");
            stop();
        }

        int sitesCount = taskList.size();
        //сперва создать карту <url-сайта,cстатус> и выставить всем сайтам INDEXING
        HashMap<String, String> siteStatus = new HashMap<>(sitesCount);//<url,status>
        for (String key : taskList.keySet()){
            siteStatus.put(key, IndexingStatus.INDEXING.toString());
        }
        //перебирать список задач до тех пор, пока он не опустеет
        //Из-за сложностей с синхронизацией - обнаруженный опустевший список удаляется из карты после очередного прохода по ней.
        while (true){
            String keyForDeleteWhenListIsEmpty = "";
            for (Map.Entry<String, List<RecursiveAction>> item : taskList.entrySet()){
                if (item.getValue().isEmpty()){
                    keyForDeleteWhenListIsEmpty = item.getKey();
                    break;
                }

                if (findCompletedTaskWithExc( item.getValue() ))
                { siteStatus.put(item.getKey(), IndexingStatus.FAILED.toString()); }
            }
            if (!keyForDeleteWhenListIsEmpty.isEmpty()){
                taskList.remove(keyForDeleteWhenListIsEmpty);
            }

            if (taskList.isEmpty()) { break; }
        }

        for (Map.Entry<String, String> item : siteStatus.entrySet()){
            SiteEntity siteEntity = siteService.findByUrl(item.getKey());
            if (item.getValue().equals(IndexingStatus.FAILED.toString())){
                siteEntity.setStatus(IndexingStatus.FAILED.toString());
                siteService.saveSite(siteEntity);
            } else if (siteEntity.getStatus().equals(IndexingStatus.INDEXING.toString())) {
                siteEntity.setStatus(IndexingStatus.INDEXED.toString());
                siteService.saveSite(siteEntity);
            }
        }
    }


    //удаление отработавших задач. Вернуть true если есть прерванные.
    private boolean findCompletedTaskWithExc(List<RecursiveAction> list){
        boolean result = false;
        synchronized (list) {
            Iterator<RecursiveAction> iterator = list.iterator();
            while (iterator.hasNext()) {
                RecursiveAction ra = iterator.next();
                if (ra.isDone()) {
                    if (ra.isCompletedAbnormally()) {
                        result = true;
                        PageParser pp = (PageParser) ra;
                        String cause = "-";
                        if (ra.isCancelled()){cause="canceled";}
                        else{cause="excepted";}
                        String msg = "uncompleted task: " + pp.getPageUrl() + " cause:" + cause;
                        log.indLog(msg, "error");

                    }
                    iterator.remove();
                }
            }
        }
        return result;
    }


    //остановить индексацию
    public boolean stop(){
        if (!isRunning){
            log.indLog("stop indexing impossible", "info");
            return false;
        }
        isRunning = false;
        //команда на завершение пула и его ожидание
        pool.shutdown();
        while (true){
            if (pool.isShutdown()){
                log.indLog("indexing was stopped", "info");
                return true;
            }
        }

    }


    //установить статус всем сайтам
    private void setAllSiteStatus(String status){
        for (Site item : configAppl.getSites()){
            SiteEntity site = siteService.findByUrl(item.getUrl());
            site.setStatus(status);
            site.setStatusTime(LocalDateTime.now());
            siteService.saveSite(site);
        }
    }


    //удалить все данные из БД
    private void clearDB(){
        log.indLog("before clear DB", "info");
        pageService.clear();//page имеет в поле site - ее надо первой грохать
        siteService.clear();
        lemmaService.clear();
        indexService.clear();
    }


    //удалить все данные указанного сайта
    private void clearSiteData(String siteUrl){
        log.indLog("before clearSiteData - " + siteUrl, "info");
        if (siteService.existUrl(siteUrl)){
            int siteId = siteService.findByUrl(siteUrl).getId();
            indexService.delAllBySiteUrl(siteUrl);
            lemmaService.delAllBySiteUrl(siteUrl);
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
