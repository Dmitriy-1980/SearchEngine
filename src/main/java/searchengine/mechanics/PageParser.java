package searchengine.mechanics;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Synchronized;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import searchengine.config.ConfigAppl;
import searchengine.model.*;
import searchengine.services.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class PageParser extends RecursiveAction {//extends RecursiveAction
    @Getter
    private final String pageUrl;
    private SiteEntity site; //объект "сайт" - формируется при чтении главной страницы. После передается параметром.
    private Document document;
    private final Vector<String> linksSet; //лист ссылок всего сайта
    private final ConcurrentHashMap<String, Integer> siteLemmaMap;//леммы всего сайта с кол их вхождений
    private final ConcurrentHashMap<String, List<RecursiveAction>> taskList;//MAP с задачами
    private final ForkJoinPool pool;
    private final int deep;
    private String protocol; // протокол запроса (в виде http: или https: )
    private final ConfigAppl config;

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final LuceneService luceneService;

    private final MyLog log = new MyLog();
/*В конструкторпередаются сервисы, конфиг приложения, пулл потоков - всегда одинаково.
* deep-глубина вложенности обрабатываемой ссылки. Если deep==0 то это главная страница.
* linkSet и siteLemmaMap коллекции ссылок и лемм одного сайта.Они сквозные для всего сайта
* и нужны для контроля на уникальность ссылок и подсчета лемм без обращения к SQL-серверу.
* taskList- структурадля отслеживания состояния задач. Вполнена, остановлена, ошибка*/

public PageParser(String pageUrl, ForkJoinPool pool,
                  Vector<String> linksSet, ConcurrentHashMap<String, Integer> siteLemmaMap,
                  ConcurrentHashMap<String, List<RecursiveAction>> taskList,
                  SiteService siteService, PageService pageService, LuceneService luceneService,
                  LemmaService lemmaService, IndexService indexService,
                  int deep, SiteEntity site, ConfigAppl config){
        this.pageUrl = pageUrl;
        this.linksSet = linksSet;
        this.siteLemmaMap = siteLemmaMap;
        this.taskList = taskList;
        this.pool = pool;
        this.siteService = siteService;
        this.pageService = pageService;
        this.luceneService = luceneService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.deep = deep;
        this.site = site;
        if (deep == 1){
            linksSet.add(pageUrl);
            this.protocol = getProtocol(pageUrl);
            this.site = new SiteEntity();
            this.site.setUrl(pageUrl);
        }
        this.config = config;
    }

    @SneakyThrows
    @Override
    public void compute(){
        PageEntity page = new PageEntity();

        try{
            Thread.sleep(config.getTimeout());
        }catch (InterruptedException e){
            //System.out.println("PageParser.compute()-interrupted");
            log.parsLog("Interrupted.. ", "error");
        }

        Connection.Response siteResponse = null;
        siteResponse = getResponse();

        //если http код "не положительный"
        if (siteResponse.statusCode() >=400){
            onHttpTroubleCode(siteResponse.statusMessage());
            return;
        }

        try {
            document = siteResponse.parse();
        } catch (IOException e) {
            //System.out.println("PageParser.compute() document = siteResponse.parse()  " + e.getCause());
            String msg = "siteResponse.parse()  " + e.getCause();
            //throw new RuntimeException(e);
        }

        //получить ссылки и проверить на уникальность
        Elements links = document.select("a");
        List<String> linkOnPage = new ArrayList<>();
        for (Element item : links){
            if (item.attribute("href") == null) { continue; }
            String link = item.attribute("href").getValue();
            String FullLink = getFullLink(link);
            if (!isCorrectLink(link) || isSubdomain(FullLink)) { continue; }
            //добавление уникальных ссылок в "сквозной список сайта" и локальный список страницы
            synchronized (linksSet) {
                if (!linksSet.contains(FullLink)) {
                    linksSet.add(FullLink);
                    linkOnPage.add(FullLink);
                }
            }
        }
        //сохранение сайта, страницы, вызов лемматизатора для анализа контента
        saveCurrentSite();
        page = saveCurrentPage(siteResponse.statusCode());

        try{
            luceneGo(page);
        }catch (Exception e){
            //System.out.println("pageParser.compute.luceneGo");
            log.parsLog("luceneGo unknown error. | " + e.getMessage() , "error");
        }

        //если не достигли "предельной глубины сканирования" то переходим по каждой ссылке и "читаем" очередную страницу.
        if (deep < config.getDeepLimit() || config.getDeepLimit() == 0){
            for (String link : linkOnPage){
                PageParser pageParser = new PageParser(link, pool, linksSet, siteLemmaMap, taskList,
                        siteService, pageService, luceneService, lemmaService , indexService, deep + 1, site, config);
                //pool.submit(pageParser).fork(); //место роковой ошибки
                taskList.get(site.getUrl()).add(pageParser);//задача добавлена по ключу-url сайта
                pool.submit(pageParser);
            }
        }
    }


    //получить протокол запроса
    private String getProtocol(String url){
        if (url.startsWith("https://")){
            return  "https:";
        } else if (url.startsWith("http://")) {
            return  "http:";
        } else {
            return "<>";
        }
    }

    //проверить ссылку на соответствие требуему виду (
    private boolean isCorrectLink(String url){
        if (url.startsWith("/") && url.length() > 1){ return true; }
        if (url.startsWith("//") && url.length() > 5){ return true; }
        if (url.startsWith("http://") && url.length() > 10){ return true; }
        if (url.startsWith("https://") && url.length() > 11){ return true; }
        return false;
    }

    //получить название сайта
    private String getSiteName(Document site){
        return site.select("title").get(0).text();
    }

    //получить локальный адрес страницы
    private String getLocalUrl(String fullUrl){
        //вытаскивает локальную ссылку из полной.
        //В отсутствии поддоменов нужно т олько адрес сайта обрезать
        return fullUrl.substring(site.getUrl().length());
    }

    //получить полную ссылку
    private String getFullLink(String link){
        if (link.startsWith("/")){ return site.getUrl() + link; }
        if (link.startsWith("//")){ return protocol + link; }
        return link;
    }

    //является ли поддоменом (домен- адрес сайта)
    private boolean isSubdomain(String url){
        if (config.isReadSubDomain()) {
            return false;
        }


        if (url.startsWith(site.getUrl())){
            return false;
        } else {
            return true;
        }
    }

    //получить ответ от сйта в виде Connection.Response
    //@SneakyThrows
    private Connection.Response getResponse(){
        try{
            return Jsoup.connect(pageUrl)
                    .ignoreHttpErrors(true)
                    .userAgent(config.getUserAgent())
                    .referrer(config.getReferer())
                    .timeout(config.getResponseWait())
                    .execute();
        }catch (Exception ex){
            String msg = "getResponse " + pageUrl + ". " + ex.getCause();
            log.parsLog(msg, "error");
            return null;
        }
    }

    //*сохранение в БД при ошибках или положительных событиях:*/

    //код http >=400
    private void onHttpTroubleCode(String msg){
        site.setStatusTime(LocalDateTime.now());
        site.setStatus(IndexingStatus.FAILED.toString());
        site.setLastError(msg);
        siteService.saveSite(site);
        log.parsLog(msg, "warn");
    }

    //сохранение текущего сайта при отсутствии ошибок
    private void saveCurrentSite(){
        if (deep == 1) {//парсим главную страницу
            site.setName(getSiteName(document));
            site.setStatus(IndexingStatus.INDEXING.toString());
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("");
        }
        else{//страница не главная, сущность SiteEntity передана параметром
            site.setStatusTime(LocalDateTime.now());
        }
        site = siteService.saveSite(site);
    }

    //сохранить текущуюю страницу при отсутствии ошибок
    private PageEntity saveCurrentPage(int code){
        PageEntity page = new PageEntity();
        page.setCode(code);
        if (deep == 1){ page.setPath("/"); }
        else { page.setPath(getLocalUrl(pageUrl)); }
        page.setContent(document.toString());
        //page.setContent(Thread.currentThread().getName());
        page.setSiteId(site);
        try {
            saveCurrentSite();//обновление времени
            return pageService.savePage(page);
        }catch (Exception e){
            //System.out.println(">>> ош.сохр.стр. " + pageUrl);
            log.parsLog("savePage error : " + pageUrl, "error");
        }
        return null;
    }

    //сохранить (или обновить) лемму для текущего сайта
    private LemmaEntity saveLemma(String lemma, int frequancy){
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setFrequency(frequancy);
        lemmaEntity.setLemma(lemma);
        lemmaEntity.setSiteId(site.getId());
        return lemmaService.saveLemma(lemmaEntity);
    }

    //сохранить индекс
    private void saveIndex(int lemmaId, int lemmaCount, int pageId){
    IndexEntity indexEntity = new IndexEntity();
    indexEntity.setPageId( pageId );
    indexEntity.setRank(lemmaCount);
    indexEntity.setLemmaId(lemmaId);

    indexService.saveIndex(indexEntity);
    }

    //lucene
    private void luceneGo(PageEntity page){
        LemmaEntity lemma = new LemmaEntity();
        lemma.setSiteId( site.getId() );

        HashMap<String,Integer> pageLemmaMap = luceneService.getLemmaMap(document.text());//карта лемм и кол по одной странице
        Iterator<Map.Entry<String, Integer>> iterator = pageLemmaMap.entrySet().iterator();

        while (iterator.hasNext()){
            boolean isNewLemma = false;//флаг -содержит карта(и бд) лемму или нет
            Map.Entry<String, Integer> item = iterator.next();
            //синхро-блок проверки на новизну леммы. Блок минимизирован.
            synchronized (siteLemmaMap) {
                if (!siteLemmaMap.containsKey(item.getKey())) {
                    siteLemmaMap.put(item.getKey(), 1 );
                    isNewLemma = true;
                }
            }

            if (isNewLemma)
            {
                lemma = saveLemma(item.getKey(), 1);
            }
            else
            {   //синхро-блок увеличения кол страниц с даной леммой
                synchronized (siteLemmaMap){
                    int currentCount = siteLemmaMap.get(item.getKey());
                    siteLemmaMap.put(item.getKey(), currentCount + 1);
                }
                //и увеличим счетчик для данной леммы в БД
                lemma = lemmaService.incrementFrequency(site.getId(), item.getKey());
            }
            //и сохранение индекса
            saveIndex(lemma.getId(), item.getValue(), page.getId());

        }

    }



}
