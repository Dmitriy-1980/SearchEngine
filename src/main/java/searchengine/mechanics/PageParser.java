package searchengine.mechanics;

import lombok.Getter;
import lombok.Setter;
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
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class PageParser extends RecursiveAction {//extends RecursiveAction
    @Getter
    private final String pageUrl;
    @Setter
    private boolean onlyThisPage = false; //флаг показывающи, что индексация только этой страницы
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
* taskList- структура для отслеживания состояния задач. Вполнена, остановлена, ошибка*/

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
            this.protocol = U.getProtocol(pageUrl);
            this.site = new SiteEntity();
            this.site.setUrl(pageUrl);
        }
        this.config = config;
    }

    @SneakyThrows
    @Override
    public void compute(){
    log.parsLog(MessageFormat.format("PageParser.compute(): url:{0}, deep:{1}, onlyThisPage:{2}, linksSet.size():{3}, siteLemmaMapSite():{4}",
                 pageUrl,deep,onlyThisPage,linksSet.size(),siteLemmaMap.size()), "info");

        PageEntity page = new PageEntity();

        try{
            Thread.sleep(config.getTimeout());
        }catch (InterruptedException e){
            log.parsLog("PageParser: Interrupted.. ", "error");
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
            String msg = "siteResponse.parse()  " + e.getCause();
        }

        //получить ссылки и проверить на уникальность
        Elements links = document.select("a");
        List<String> linkOnPage = new ArrayList<>();
        for (Element item : links){
            if (item.attribute("href") == null) { continue; }
            String link = item.attribute("href").getValue();
            if (!U.isCorrectLink(link) || !U.inThisSite(link,site.getUrl())) { continue; }
            String FullLink = U.getFullUrl(link, site.getUrl(), protocol);
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
            log.parsLog("PageParser.luceneGo(): неизвестн.ошибка. | " + e.getMessage() , "error");
        }

        //если нет задачи индексировать только эту страницу,
        // и не достигли "предельной глубины сканирования" то переходим по каждой ссылке и "читаем" очередную страницу.
        if ( (!onlyThisPage) &&
                (deep < config.getDeepLimit() || config.getDeepLimit() == 0) )
        {
            for (String link : linkOnPage){
                PageParser pageParser = new PageParser(link, pool, linksSet, siteLemmaMap, taskList,
                        siteService, pageService, luceneService, lemmaService , indexService, deep + 1, site, config);
                taskList.get(site.getUrl()).add(pageParser);//задача добавлена по ключу-url сайта
                pool.submit(pageParser);
            }
        }
    }



    /**Получить имя сайта из его кода.
     * @param document структуированный код сайта.*/
    private String getSiteName(Document document){
        return document.select("title").get(0).text();
    }


    //получить ответ от сйта в виде Connection.Response
    private Connection.Response getResponse(){
        try{
            return Jsoup.connect(pageUrl)
                    .ignoreHttpErrors(true)
                    .userAgent(config.getUserAgent())
                    .referrer(config.getReferer())
                    .timeout(config.getResponseWait())
                    .execute();
        }catch (Exception ex){
            log.parsLog("PageParser.getResponse("+pageUrl+")" + ex.getCause(), "error");
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
        log.parsLog("http код: "+msg, "warn");
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
        else { page.setPath(U.getLocalUrl(pageUrl, site.getUrl() ) ); }
        page.setContent(document.toString());
        //page.setContent(Thread.currentThread().getName());
        page.setSiteId(site);
        try {
            saveCurrentSite();//обновление времени
            return pageService.savePage(page);
        }catch (Exception e){
            //System.out.println(">>> ош.сохр.стр. " + pageUrl);
            log.parsLog("PageParser.saveCurrentPage(): ошибка сохранения " + pageUrl, "error");
        }
        return null;
    }

    //сохранить (или обновить) лемму для текущего сайта
    private LemmaEntity saveLemma(String lemma){
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setFrequency(1);
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
            if (isNewLemma) { lemma = saveLemma(item.getKey()); }
            else
            {   //синхро-блок увеличения кол страниц с даной леммой
                synchronized (siteLemmaMap){
                    int currentCount = siteLemmaMap.get(item.getKey());
                    siteLemmaMap.put(item.getKey(), currentCount + 1);
                }
                //и увеличим счетчик для данной леммы в БД
                lemma = lemmaService.getBySiteIdAndLemma(site.getId(), item.getKey());
                lemma = lemmaService.changeFrequency(lemma.getId(), 1);
            }
            //и сохранение индекса
            saveIndex(lemma.getId(), item.getValue(), page.getId());
        }
    }



}
