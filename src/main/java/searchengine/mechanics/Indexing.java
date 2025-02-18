package searchengine.mechanics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.config.ConfigAppl;
import searchengine.config.Site;
import searchengine.dto.CommandResult;
import searchengine.model.IndexingStatus;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
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
    private final PageRepository pageRepository;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final LuceneService luceneService;
    private final MyLog log = new MyLog();
    private final ForkJoinPool pool = new ForkJoinPool();
    @Setter
    private Boolean isRunning = false;
    /**taskList-карта, где KEY = url сайта, а VALUE = список задачь соотв. этому сайту */
    private final ConcurrentHashMap<String, List<RecursiveAction>> taskList = new ConcurrentHashMap<>();

    /**Запуск индексации по команде /api/startIndexing.*/
    public CommandResult startFromList(){
        if (notMayStart()){
            throw new IllegalStateException("Индексация уже запущена. Попробуйте позже."); }

        clearDB();
        long start = System.currentTimeMillis();
        for (Site site : configAppl.getSites()){
            goSiteIndex(site.getUrl(), false, true);//запуск индексации очередного сайта
        }
        WaitOfIndexEnd waitOfIndexEnd = new WaitOfIndexEnd(this, start);
        pool.submit(waitOfIndexEnd);
        return new CommandResult(true);
    }


    /**<pre>Попытка запуска индексации по команде контроллера (/api/indexPage).</pre>
     * @param url адрес индексируемой страницы.
     * @return CommandResult объект содержащий результат попытки выполнения.*/
    public CommandResult startAdditionalIndexing(String url){
        if (notMayStart()){
            throw new IllegalStateException("Индексация уже запущена. Попробуйте позже."); }

        long start = System.currentTimeMillis();
        String address = checkUrl(url);
        switch (address){
            case "error" -> {
                isRunning = false;
                throw new IllegalStateException("\"Адрес не принадлежит сайтам из списка.");
            }
            case "site"-> {
                clearSiteData(url);
                goSiteIndex(url, true, true);
            }
            default ->{
                if ( pageService.isExistUrl(address) )
                    { clearPageData(url); }
                goSiteIndex(url, true, false);
            }
        }

        WaitOfIndexEnd waitOfIndexEnd = new WaitOfIndexEnd(this, start);
        pool.submit(waitOfIndexEnd);
        return new CommandResult(true);
    }


    /**Индексация одного адреса.
     * <p>Для запуска индексации всего сайта, его первой страницы, просто отдельной страницы
     * нужны разные параметры. Тут они и формируются.</p>
     * @param url дрес страницы,
     * @param onlyThisPage если true то по ссылкам не проваливаться,
     * @param isFirstPage если true то это главная страница сайта
     * <p>Логические параметры обеспечивают разницу параметров передаваемых в задачу парсинга страницы.</p>*/
    private void goSiteIndex(String url, boolean onlyThisPage, boolean isFirstPage){
        int deep = 1;
        SiteEntity siteEntity = null;
        Vector<String> linksSet = new Vector<>(); //уникальный список ссылок со всего сайта
        ConcurrentHashMap<String,Integer> siteLemmaMap = new ConcurrentHashMap<>(); //уникальный список лемм с кол их вхождений для всего сайта
        //если индексация только одной страницы
        if (onlyThisPage){
            deep = isFirstPage ? 1 : 2; //для не первой страницы - любое отличное от 1
            //получить сущность сайта
            for (Site site : configAppl.getSites()){
                if (url.toLowerCase().contains(site.getUrl())){
                    siteEntity = siteService.findByUrl(site.getUrl());
                }
            }
            if (siteEntity==null){
                log.indLog("Indexing.goSinglePageIndex(): сайт в БД по url не найден.", "error");
                return ;
            }
            //получить список ссылок со всего сайта
            linksSet = new Vector<>( pageService.getAllLinksBySiteId(siteEntity) );
            //получить карту <lemma,frequency> для сайта
            siteLemmaMap = new ConcurrentHashMap<>();
            for (LemmaEntity item : lemmaService.getAllLemmasBySiteId(siteEntity.getId())){
                siteLemmaMap.put(item.getLemma(), item.getFrequency());
            }
        }
        //и по полученным параметрам создать задачу
            PageParser pageParser = new PageParser(url, pool,
                    linksSet, siteLemmaMap, taskList,
                    siteService, pageService, luceneService, lemmaService, indexService,
                    deep, siteEntity, configAppl);
            pageParser.setOnlyThisPage(onlyThisPage);

            String urlSite = Utilites.pagesSite(url, configAppl.getSites());
            taskList.put(urlSite, new Vector<>());
            taskList.get(urlSite).add(pageParser);
            pool.submit(pageParser);
    }



    /**Ожидание окончания индексации.
     * Этот метод запускаетсяиз специальной задачи WaitOfIndexEnd которая висит в пуле потоков.
     * */
    public void waitOfIndexingEnd(){
        try {
            Thread.sleep(1000);
        }catch (InterruptedException e){
            log.indLog("Interrupted.. " + e.getMessage(), "error");
            stop();
            return;
        }

        //перебирать список задач до тех пор, пока он не опустеет
        while (!taskList.isEmpty()){
            Iterator<Map.Entry<String,List<RecursiveAction>>> taskListIterator = taskList.entrySet().iterator();
            while (taskListIterator.hasNext()){
                Map.Entry<String,List<RecursiveAction>> tasksOfSite = taskListIterator.next();
                findCompletedTaskWithExc(tasksOfSite.getValue());
                if (tasksOfSite.getValue().isEmpty()) {
                    SiteEntity siteEntity = siteService.findByUrl(tasksOfSite.getKey());
                    if (siteEntity.getStatus().equals(IndexingStatus.INDEXING.toString())){
                    siteEntity.setStatus(IndexingStatus.INDEXED.toString());
                    siteService.saveSite(siteEntity);
                    }
                    taskListIterator.remove();
                }
            }
        }
    }


    /**Перебирает все задачи из списка. Найдя законченную удаляет ее.
     * Если попадется "неправильно завершенная" то логгирует.
     * @param list - список задач соответствующих одному конкретному сайту.*/
    private void findCompletedTaskWithExc(List<RecursiveAction> list){
        synchronized (list) {
            Iterator<RecursiveAction> iterator = list.iterator();
            while (iterator.hasNext()) {
                RecursiveAction ra = iterator.next();
                if (ra.isDone()) {
                    if (ra.isCompletedAbnormally()) {
                        PageParser pp = (PageParser) ra;
                        String cause = "-";
                        if (ra.isCancelled())
                            { cause="canceled"; }
                        else
                            { cause="excepted"; }
                        String msg = "uncompleted task: " + pp.getPageUrl() + " cause:" + cause;
                        log.indLog(msg, "error");
                    }
                    iterator.remove();
                }
            }
        }
    }


    /**Остановка текущей индексации.
     * Монопольно обращается к списку задач индексации (taskList),
     * перебирает все задачи и останавливает методом task.cancel()
     * @return CommandResult с соотв результатом*/
    public CommandResult stop(){
        if (!isRunning){
            throw new IllegalStateException("Индексация не запущена.");
        }
        //команда на завершение пула и его ожидание
        synchronized (taskList){
            for (Map.Entry<String, List<RecursiveAction>> tasksOfSite : taskList.entrySet()){
                SiteEntity siteEntity = siteService.findByUrl(tasksOfSite.getKey());
                siteEntity.setStatus(IndexingStatus.FAILED.toString());
                siteEntity.setStatusTime(LocalDateTime.now());
                siteEntity.setLastError("Индексация остановлена пользователем");
                siteService.saveSite(siteEntity);
                for (RecursiveAction task : tasksOfSite.getValue()){
                    task.cancel(true);
                }
            }
        }
        isRunning = false;
        return new CommandResult(true);
    }


    /**Удаляет ВСЕ данные из БД*/
    private void clearDB(){
        pageService.clear();
        siteService.clear();
        lemmaService.clear();
        indexService.clear();
    }


    /**Удаляет все данные связанные с указанным сайтом.
     * @param siteUrl адрес сайта, данные которого нужно удалить.*/
    private void clearSiteData(String siteUrl){
        if (siteService.existUrl(siteUrl)){
            SiteEntity siteEntity = siteService.findByUrl(siteUrl);
            int siteId = siteEntity.getId();
            indexService.delAllBySite(siteEntity);
            lemmaService.delAllBySiteUrl(siteUrl);
            pageService.delAllBySiteId(siteEntity);
            siteService.delById(siteId);
        }
    }


    /**Удаляет все данные связанные с указанной страницей.
     * @param fullUrl адрес страницы данные которой нужно удалить.*/
    private void clearPageData(String fullUrl){
        String pageUrl = Utilites.getLocalUrl(fullUrl, configAppl.getSites());
        String siteUrl = Utilites.pagesSite(fullUrl, configAppl.getSites());

        //найти id сайта с этой страницей
        SiteEntity siteEntity = siteService.findByUrl(siteUrl);
        // найти id зачищаемой страницы.
        int pageId;
        try{
            pageId = pageService.getIdByPathAndSite(pageUrl, siteEntity);
           }catch (Exception ex){
            log.indLog("Indexing.clearPageData():"+pageUrl + " такая страница не найдена в БД: ex=" + ex.getCause(), "error");
            return;
        }
        // найти список lemma_id связанных с page_id зачищаемой страницы
        List<Integer> listLemmaId = indexService.getAllLemmaIdByPageId(pageId);
        if (listLemmaId.isEmpty()){
            log.indLog("не найдено ни одной леммы по page_id (" + pageId + ")", "error");
            return;
        }
        //уменьшить каждой лемме frequency на 1. (если =0 то удалить)
        lemmaService.frequencyDecrement(listLemmaId);
        //удалить связанные со страницей индексы
        indexService.delAllByPageId(pageId);
        //удалитьсаму страницу
        pageService.delById(pageId);
    }


    /**Проверяет можно ли начать индексацию в данный момент.
     * Если индексация не идет, то разрешаем и флаг переводится в True.
     * Возвращается значение обратное флагу. Идет индексация = нельзя начать индексацию.*/
    public synchronized boolean  notMayStart(){
        if (isRunning){
            return true;
        } else{
            isRunning = true;
            return false;
        }
    }


    /**Проверка адреса на принадлежность заданному списку сайтов.
     * @param inputUrl проверяемый url
     * @return
     * <p>=error, если ссылка не принадлежит сайту из списка</p>
     * <p>=site, если ссылка на главную страницу сайтаиз списка</p>
     * <p>=сама ссылка, если не главная страница сайта из списка*</p>*/
    public String checkUrl(String inputUrl){
        String localUrl = Utilites.getLocalUrl(inputUrl,configAppl.getSites());
        if (localUrl.isEmpty()) { return "error"; }
        if (inputUrl.equalsIgnoreCase(localUrl)) { return "site"; }
        return localUrl;
    }



}
