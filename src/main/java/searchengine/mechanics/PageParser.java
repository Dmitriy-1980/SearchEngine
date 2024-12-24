package searchengine.mechanics;

import lombok.SneakyThrows;
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

        Thread.sleep(config.getTimeout());
        Connection.Response siteResponse = getResponse();

        //если http код "не положительный"
        if (siteResponse.statusCode() >=400){
            onHttpTroubleCode(siteResponse.statusMessage());
            return;
        }

        document = siteResponse.parse();

        //получить ссылки и проверить на уникальность
        Elements links = document.select("a");
        List<String> linkOnPage = new ArrayList<>();
        for (Element item : links){
            if (item.attribute("href") == null) { continue; }
            String link = item.attribute("href").getValue();
            link = getFullLink(link);
            if (!isCorrectLink(link) || isSubdomain(link)) { continue; }
            //добавление уникальных ссылок в "сквозной список сайта" и локальный список страницы
            synchronized (linksSet) {
                if (!linksSet.contains(link)) {
                    linksSet.add(link);
                    linkOnPage.add(link);
                }
            }
        }
        //сохранение сайта, страницы, вызов лемматизатора для анализа контента
        saveCurrentSite();
        page = saveCurrentPage(siteResponse.statusCode());
        luceneGo(page);

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
        if (url.equals(site.getUrl())){
            return false;
        }

        if (url.length() < 2){
            return false;
        }

        if (url.contains("#")){
            return false;
        }

        if (url.startsWith("https://") || (url.startsWith("http://"))){
            return true;
        }

        if (url.startsWith("/") && url.length()>1){
            return true;
        }

        int k = site.getUrl().indexOf("//");
        if ( url.startsWith( site.getUrl().substring(k) )){
            return true;
        }

        return false;
    }

    //получить название сайта
    private String getSiteName(Document site){
        return site.select("title").get(0).text();
    }

    //получить локальный адрес страницы
    private String getLocalUrl(String fullUrl){
        String url = fullUrl;
        if (!fullUrl.endsWith("/")){
            url = fullUrl + "/";
        }

        //если первая страница
        if (deep == 1) {
            return "/";
        }

        //полная ссылка
        if (fullUrl.startsWith(site.getUrl())){
            return fullUrl.substring(site.getUrl().length());
        }

        //ссылка без протокола
        if ((url.startsWith("//"))){
            return (protocol + url).substring(site.getUrl().length());
        }

        //если ссылка локальная
        if (url.startsWith("/")){
            return url;
        }

        return url;
    }

    //получить полную ссылку
    private String getFullLink(String link){
        String rez = "";

        if (link.startsWith(site.getUrl()) ||
                link.startsWith("https://") || link.startsWith("http://")){
            rez = link;
        }
        else if (link.startsWith("//")) {
            int pos = site.getUrl().indexOf("//");
            rez = site.getUrl().substring(0,pos) + link;
        }
        else if (link.startsWith("/")) {
            rez = site.getUrl() + link.substring(1);
        }
        return rez;
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
    @SneakyThrows
    private Connection.Response getResponse() throws IOException {
        return Jsoup.connect(pageUrl)
                .ignoreHttpErrors(true)
                .userAgent(config.getUserAgent())
                .referrer(config.getReferer())
                .timeout(config.getResponseWait())
                .execute();
    }

    //*сохранение в БД при ошибках или положительных событиях:*/

    //код http >=400
    private void onHttpTroubleCode(String msg){
        site.setStatusTime(LocalDateTime.now());
        site.setStatus(IndexingStatus.FAILED.toString());
        site.setLastError(msg);
        siteService.saveSite(site);
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
        page.setPath(getLocalUrl(getLocalUrl(pageUrl)));
        page.setContent(document.toString());
        page.setSiteId(site);
        try {
            saveCurrentSite();//обновление времени
            return pageService.savePage(page);
        }catch (Exception e){
            System.out.println(">>> ош.сохр.стр. " + pageUrl);
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

        HashMap<String,Integer> pageLemmaMap = luceneService.getLemmaMap(document.text());

        for (Map.Entry<String, Integer> item : pageLemmaMap.entrySet()){
            if (siteLemmaMap.containsKey(item.getKey())) {
                //обновить лемм. кол.из siteLemmaMap+кол.из pageLemmaMap.  И индекс.
                int lemmaCount = siteLemmaMap.get(item.getKey());
                lemma = lemmaService.update(item.getKey(), lemmaCount + 1); //count+1
                siteLemmaMap.put(item.getKey(), item.getValue() + lemmaCount);
            } else {
                //создать лемму по данным из pageLemmaMap. И индекс.
                lemma = saveLemma(item.getKey(), 1);
                siteLemmaMap.put(item.getKey(), item.getValue());
            }
            saveIndex(lemma.getId(), item.getValue(), page.getId());
        }
    }




}
