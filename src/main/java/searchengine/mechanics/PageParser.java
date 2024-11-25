package searchengine.mechanics;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import searchengine.config.Config;
import searchengine.model.*;
import searchengine.services.IndexServiceImpl;
import searchengine.services.LemmaServiceImpl;
import searchengine.services.PageServiceImpl;
import searchengine.services.SiteServiceImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class PageParser extends RecursiveAction {
    private final String pageUrl;
    private SiteEntity site; //объект "сайт" - формируется при чтении главной страницы. После передается параметром.
    private Document document;
    private HashSet<String> linksSet;
    private final ForkJoinPool pool;
    private final int deep;
    private String protocol; // протокол запроса (в виде http: или https: )
    private final Config config;

    private final SiteServiceImpl siteService;
    private final PageServiceImpl pageService;
    private final LemmaServiceImpl lemmaService;
    private final IndexServiceImpl indexService;


public PageParser(String pageUrl, HashSet<String> linksSet,
                  ForkJoinPool pool,
                  SiteServiceImpl siteService, PageServiceImpl pageService,
                  LemmaServiceImpl lemmaService, IndexServiceImpl indexService,
                  int deep, SiteEntity site, Config config){
        this.pageUrl = pageUrl;
        this.linksSet = linksSet;
        this.pool = pool;
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.deep = deep;
        this.site = site;
        if (deep == 1){
            linksSet.add(pageUrl);
            this.protocol = getProtocol(pageUrl);
        }
        this.config = config;
    }

    //@SneakyThrows
    @Override
    public void compute(){
        try {
            readPage();
        }catch (Exception e){
            System.out.println("**** Ошибка при работе со страницей ." + pageUrl);
            e.printStackTrace();
        }
    }


    private void readPage(){
//если deep=1 то это главная страница, значит нужно создать кроме page еще и  site
        if (deep == 1){
            site = new SiteEntity();
        }
        PageEntity page = new PageEntity();
        LemmaEntity lemma = new LemmaEntity();
        IndexEntity index = new IndexEntity();

        //непроснувшийся поток:
        try {
            Thread.sleep(config.getTimeout());
        } catch (InterruptedException e){
            onInterruptedException(e);
            return;
        }

        //ошибка получения siteResponse
        Connection.Response siteResponse = null;
        try {
            siteResponse = getResponse();
        }
        catch (IOException e){
            onSiteResponseIsNull(e);
            return;
        }

        //если http код "не положительный"
        if (siteResponse.statusCode() >=400){
            onHttpTroubleCode(siteResponse.statusCode());
            return;
        }

        //далее считаем, что ответ от сайта получен
        StringBuilder content = new StringBuilder();

        //при ошибке парсинга:
        try{
            document = siteResponse.parse();
        }
        catch (IOException e){
            onParsingError(e);
            return;
        }

        //сайт прочитан и Document получен.
        if (deep == 1 ){
            saveCurrentSite();
        }

        //получить контент (без тегов логично)
        Elements elements = document.select("*");
        for (Element tag : elements){
            content.append(tag.text()).append(" ");
        }

        //получить ссылки и проверить на уникальность
        elements = document.select("a");
        List<String> linkOnPage = new ArrayList<>();
        for (Element item : elements){
            if (item.attribute("href") == null){
                continue;
            }
            String link = item.attribute("href").getValue();
            link = getFullLink(link);
            if (!isCorrectLink(link) || isSubdomain(link)){
                continue;
            }
            synchronized (linksSet) {
                if (linksSet.add(link)) {
                    linkOnPage.add(link);
                }
            }
        }

        //сохранить собранную о странице информацию
        saveCurrentPage(siteResponse.statusCode(), content.toString());

        //если не достигли "предельной глубины сканирования" то переходим по каждой ссылке и "читаем" очередную страницу.
        if (deep < config.getDeepLimit()){
            for (String link : linkOnPage){
                PageParser pageParser = new PageParser(link, linksSet, pool,
                        siteService, pageService, lemmaService , indexService, deep + 1, site, config);
                pool.submit(pageParser).fork();
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
    //непроснувшийся поток
    private void onInterruptedException(Exception e){
        if (deep == 1){
            site.setUrl(pageUrl);
            site.setName("-");
            site.setStatus(IndexingStatus.FAILED.toString());
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Поток не вышел из сна. InterruptedException. " + e.getMessage());
            siteService.addEntity(site);
        }
        else{
            PageEntity page = new PageEntity();
            page.setSiteId(site);
            page.setContent("-");
            page.setCode(520); //неизвестная ошибка
            page.setPath(pageUrl);
            pageService.addEntity(page);

            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Пото не вышел из сна. InterruptedException. " + e.getMessage());
            siteService.addEntity(site);
        }
    }

    //не удалось получить SiteResponse
    private void onSiteResponseIsNull(Exception e){
        System.out.println("Ошибка получения объекта SiteResponse. (Возможны проблемы с сертификацией.) " + e.getMessage());
        String msg = "Ошибка получения объекта SiteResponse. (Возможны проблемы с сертификацией.) " +
                e.getMessage();
        System.out.println(msg);
        if (deep == 1){
            site.setStatusTime(LocalDateTime.now());
            site.setUrl(pageUrl);
            site.setName("-");
            site.setStatus(IndexingStatus.FAILED.toString());
            site.setLastError(msg);
            siteService.addEntity(site);
        }
        else{
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(IndexingStatus.FAILED.toString());
            site.setLastError(msg);
            siteService.addEntity(site);

            PageEntity page = new PageEntity();
            page.setContent("-");
            page.setPath(pageUrl);
            page.setCode(520);
            page.setSiteId(site);
            pageService.addEntity(page);
        }
    }

    //код http >=400
    private void onHttpTroubleCode(int code){
        System.out.println("Сайт недоступен. Код ошибки: " + code);
        if (deep == 1){
            site.setUrl(pageUrl);
            site.setLastError("Сайт недоступен. Код ошибки: " + code);
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(IndexingStatus.FAILED.toString());
            site.setName("-");
            siteService.addEntity(site);
        }
        else{
            PageEntity page = new PageEntity();
            page.setCode(code);
            page.setContent("-");
            page.setPath(pageUrl);
            page.setSiteId(site);
            pageService.addEntity(page);
        }
    }

    //ошибка парсинга
    private void onParsingError(Exception e){
        System.out.println("Ошибка парсинга. Не удалось получить Jsoup.nodes.Document. Ошибка: " + e.getMessage());
        if (deep == 1){
            site.setLastError("Ошибка парсинга. Не удалось получить Jsoup.nodes.Document. Ошибка: " + e.getMessage());
            site.setName("-");
            site.setStatus(IndexingStatus.FAILED.toString());
            site.setStatusTime(LocalDateTime.now());
            site.setUrl(pageUrl);
            siteService.addEntity(site);
        }
        else {
            site.setLastError("Ошибка парсинга чтраницы \"" + pageUrl + "\". Не удалось получить Jsoup.nodes.Document." +
                    " Ошибка: " + e.getMessage());
            site.setStatusTime(LocalDateTime.now());
            siteService.addEntity(site);

            PageEntity page = new PageEntity();
            page.setCode(520);
            page.setContent("-");
            page.setPath(getLocalUrl(pageUrl));
            page.setSiteId(site);
            pageService.addEntity(page);
        }
    }

    //сохранение текущего сайта при отсутствии ошибок
    private void saveCurrentSite(){
        site.setStatus(IndexingStatus.INDEXING.toString());
        site.setName( getSiteName(document) );
        site.setStatusTime(LocalDateTime.now());
        site.setUrl(pageUrl);
        //siteRep.save(site);
        siteService.addEntity(site);
    }

    //сохранить текущуюю страницу при отсутствии ошибок
    private void saveCurrentPage(int code, String content){
        PageEntity page = new PageEntity();
        page.setCode(code);
        //page.setContent(content);
        page.setContent("thread " + Thread.currentThread().getName() + "  time " + LocalDateTime.now());
        //page.setPath(getLocalUrl(getLocalUrl(pageUrl)));
        page.setPath(getLocalUrl(getLocalUrl(pageUrl) + " " + Thread.currentThread().getName()));
        page.setSiteId(site);
        try {
            pageService.addEntity(page);
        }catch (Exception e){
            System.out.println("Ошибка сохранения " + page.getPath() + " ");
        }
    }
}
