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
    /**метка - идет ли в данный момент индексирование*/
    private Boolean isRunning = false;
    //карта: <url-сайта , список задач по нему>
    /**taskList-карта, где KEY = url сайта, а VALUE = список задачь соотв. этому сайту */
    private final ConcurrentHashMap<String, List<RecursiveAction>> taskList = new ConcurrentHashMap<>();

    /**Запуск индексации по команде /api/startIndexing.*/
    public CommandResult startFromList(){
        log.indLog("Indexing.startFromList() ", "info");
        if (notMayStart()){
            return new CommandResult(false, "Индексация уже запущена.");
        }
        clearDB();//зачистить БД
        long start = System.currentTimeMillis();
        for (Site site : configAppl.getSites()){
            goSiteIndex(site.getUrl(), false, true);//запуск индексации очередного сайта
        }
        WaitOfIndexEnd waitOfIndexEnd = new WaitOfIndexEnd(this, start);
        pool.submit(waitOfIndexEnd);
        return new CommandResult(true, "");
    }


    /**<pre>Попытка запуска индексации по команде контроллера (/api/indexPage).</pre>
     * @param url адрес индексируемой страницы.
     * @return CommandResult объект содержащий результат попытки выполнения.*/
    public CommandResult startAdditionalIndexing(String url){
        log.indLog("Indexing.StartAdditionalIndexing(" + url + ")", "info");

        if (notMayStart()){
            isRunning = false;
            return new CommandResult(false, "Индексация уже запущена. Попробуйте позже.");
        }

        long start = System.currentTimeMillis();
        String address = checkUrl(url);
        switch (address){
            case "error" -> {
                isRunning = false;
                return new CommandResult(false, "Адрес не принадлежит сайтам из списка."); }
            case "site"-> {
                clearSiteData(url);
                goSiteIndex(url, true, true);
                break; }
            default ->{
                if ( pageService.isExistUrl(address) )
                    { clearPageData(url); }
                goSiteIndex(url, true, false);
                break; }
        }

        WaitOfIndexEnd waitOfIndexEnd = new WaitOfIndexEnd(this, start);
        pool.submit(waitOfIndexEnd);
        return new CommandResult(true, "");
    }


    //индексация одного сайта(адреса)
    //при индексации отдельной страницы нужно получить уже известные данные из БД
    /**Индексация одного адреса.
     * <p>Для запуска индексации всего сайта, его первой страницы, просто отдельной страницы
     * нужны разные параметры. Тут они и формируются.</p>
     * @param url дрес страницы,
     * @param onlyThisPage если true то по ссылкам не проваливаться,
     * @param isFirstPage если true то это главная страница сайта
     * <p>Логические параметры обеспечивают разницу параметров передаваемых в задачу парсинга страницы.</p>*/
    private void goSiteIndex(String url, boolean onlyThisPage, boolean isFirstPage){
        log.indLog("Indexing.goSiteIndex():"+url+", onlyThisPage="+onlyThisPage, "info");
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
                    try{
                        siteEntity = siteService.findByUrl(site.getUrl());
                    }catch (Exception e){
                        System.out.println("STOP goSiteIndex - 1");}
                    break;
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
            pageParser.setOnlyThisPage(onlyThisPage); //передача метки об индексации исключительно этой страницы

            String urlSite = U.pagesSite(url, configAppl.getSites());
            taskList.put(urlSite, new Vector<>());
            taskList.get(urlSite).add(pageParser);
            pool.submit(pageParser);
    }



    //ожидание окончания индексации (в случае единственного пула)
    //И проверка статуса. Завершенные с ошибкой отметиться в записи site могут не успеть
    /**Ожидание окончания индексации.
     * Этот метод запускаетсяиз специальной задачи WaitOfIndexEnd которая висит в пуле потоков.
     * */
    public void waitOfIndexingEnd(){
        log.indLog("Indexing.waitOfIndexingEnd()", "info");
        try {
            Thread.sleep(1000);
        }catch (InterruptedException e){
            log.indLog("Interrupted.. " + e.getMessage(), "error");
            stop();
            return;
        }

        int sitesCount = taskList.size();
        //сперва создать карту <url-сайта,cтатус> и выставить всем сайтам INDEXING
         HashMap<String, String> siteStatus = new HashMap<>(sitesCount);//<url,status>
        for (String key : taskList.keySet()){
            siteStatus.put(key, "INDEXING");
        }
        //перебирать список задач до тех пор, пока он не опустеет
        //Из-за сложностей с синхронизацией - обнаруженный опустевший список удаляется из карты после очередного прохода по ней.
        while (!taskList.isEmpty()){
            Iterator<Map.Entry<String,List<RecursiveAction>>> taskListIterator = taskList.entrySet().iterator();
            while (taskListIterator.hasNext()){
                Map.Entry<String,List<RecursiveAction>> tasksOfSite = taskListIterator.next();
                if (findCompletedTaskWithExc(tasksOfSite.getValue()))
                    { siteStatus.put(tasksOfSite.getKey(), "FAILED"); }
                if (tasksOfSite.getValue().isEmpty())
                    { taskListIterator.remove(); }
            }
        }

        for (Map.Entry<String, String> item : siteStatus.entrySet()){
            SiteEntity siteEntity = siteService.findByUrl(item.getKey());
            if (item.getValue().equals("INDEXING")){
                siteEntity.setStatus("INDEXED");

            } else {
                siteEntity.setStatus("FAILED");
            }
            siteService.saveSite(siteEntity);
        }
    }


    /**Перебирает все задачи из списка. Найдя законченную удаляет ее.
     * Если попадется "неправильно завершенная" то возвратит true.
     * @param list - список задачт соответствующих одному конкретному сайту.
     * @return boolean - найдены ли "неправильно завершенные задачи".
     * */
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


    /**Остановка текущей индексации.
     * Монопольно обращается к списку задач индексации (taskList),
     * перебирает все задачи и останавливает методом task.cancel()
     * @return CommandResult с соотв результатом*/
    public CommandResult stop(){
        if (!isRunning){
            log.indLog("Indexing.stop(): индексация не идет", "info");
            return new CommandResult(false, "Индексация не запущена.");
        }
        //команда на завершение пула и его ожидание
        synchronized (taskList){
            for (Map.Entry<String, List<RecursiveAction>> taskOfSite : taskList.entrySet()){
                for (RecursiveAction task : taskOfSite.getValue()){
                    task.cancel(true);
                }
            }
        }
        isRunning = false;
        log.indLog("Indexing.stop(): индексация остановлена", "info");
        return new CommandResult(true, "Индексация остановлена");
    }


    /**Удаляет ВСЕ данные из БД*/
    private void clearDB(){
        log.indLog("Indexing.clearDB()", "info");
        pageService.clear();//page имеет в поле site - ее надо первой грохать
        siteService.clear();
        lemmaService.clear();
        indexService.clear();
    }


    /**Удаляет все данные связанные с указанным сайтом.
     * @param siteUrl адрес сайта, данные которого нужно удалить.*/
    private void clearSiteData(String siteUrl){
        log.indLog("Indexing.clearSiteData(): " + siteUrl, "info");
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
        log.indLog("Indexing.clearPageData():" + fullUrl, "info");
        String pageUrl = U.getLocalUrl(fullUrl, configAppl.getSites());
        String siteUrl = U.pagesSite(fullUrl, configAppl.getSites());

        //найти id сайта с этой страницей
        SiteEntity siteEntity = siteService.findByUrl(siteUrl);
        int siteId = siteEntity.getId();
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
        for (Integer id : listLemmaId){
            lemmaService.changeFrequency(id, -1);
        }
        //lemmaService.frequencyDecrement(listLemmaId);

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
        String localUrl = U.getLocalUrl(inputUrl,configAppl.getSites());
        if (localUrl.isEmpty()) { return "error"; }
        if (inputUrl.equalsIgnoreCase(localUrl)) { return "site"; }
        return localUrl;
    }



}
