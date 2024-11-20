package searchengine.mechanics;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class PageParser extends RecursiveAction {
    private String pageUrl;
    private SiteEntity site; //объект "сайт" - формируется при чтении главной страницы. После передается параметром.
    private Document document;
    private HashSet linksSet;
    private ForkJoinPool pool;
    private int deep;
    private int deepLimit;
    private int timeout; //задержка между запросами к страницам сайта
    private boolean readSubDomain; //читать ли поддомены
    private String protocol; // протокол запроса (в виде http: или https: )

    private SiteRepository siteRep;
    private PageRepository pageRep;
    private LemmaRepository lemmaRep;
    private IndexRepository indexRep;


    public PageParser(String pageUrl, HashSet<String> linksSet,
                      ForkJoinPool pool,
                      SiteRepository siteRep, PageRepository pageRep,
                      LemmaRepository lemmaRep, IndexRepository indexRep,
                      int deep, int deepLimit, SiteEntity site, int timeout, boolean readSubDomain){
        if (pageUrl.endsWith("/")){
            this.pageUrl = pageUrl;
        } else{
            this.pageUrl = pageUrl + "/";
        }
        this.linksSet = linksSet;
        this.pool = pool;
        this.siteRep = siteRep;
        this.pageRep = pageRep;
        this.lemmaRep = lemmaRep;
        this.indexRep = indexRep;
        this.deep = deep;
        this.deepLimit = deepLimit;
        this.site = site;
        this.timeout = timeout;
        if (deep == 1){
            linksSet.add(pageUrl);
            this.protocol = getProtocol(pageUrl);
        }
    }

    //@SneakyThrows
    @Override
    public void compute(){

        System.out.println("thread: " + Thread.currentThread().getName());
        System.out.println(this);
        try{
            Thread.sleep(timeout);
        }catch (Exception e){
            System.out.println("не проснулся. compute() ");
        }

        try {
            readPage();
        }catch (Exception e){
            System.out.println("**** Ошибка парсинга. ");
            e.printStackTrace();
        }
    }


    private void readPage(){
//если deep=1 то это главная страница, значит нужно создать кроме page еще и  site
        if (site == null){
            site = new SiteEntity();
        }
        PageEntity page = new PageEntity();
        LemmaEntity lemma = new LemmaEntity();
        IndexEntity index = new IndexEntity();

        //непроснувшийся поток:
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e){
            if (deep == 1){
                site.setUrl(pageUrl);
                site.setStatus(IndexingStatus.FAILED.toString());
                site.setName("-");
                site.setStatusTime(LocalDateTime.now());
                site.setLastError("Пото не вышел из сна. InterruptedException. " + e.getMessage());
                siteRep.save(site);
            }
            else{
                page.setSiteId(site);
                page.setContent("-");
                page.setCode(520); //неизвестная ошибка
                page.setPath(pageUrl);
                pageRep.save(page);

                site.setStatusTime(LocalDateTime.now());
                site.setLastError("Пото не вышел из сна. InterruptedException. " + e.getMessage());
                siteRep.save(site);
            }
            return;
        }

        //ошибкаполучения siteResponse
        Connection.Response siteResponse = null;
        try {
            siteResponse = getResponse();
        }catch (Exception e){
            System.out.println("Ошибка получения объекта SiteResponse");
        }

        //если http код "отрицательный"
        if (siteResponse.statusCode() >=400){
            if (deep == 1){
                //главная стр. Сделать запись site
                site.setUrl(pageUrl);
                site.setLastError("Сайт недоступен. Ошибка: " + siteResponse.statusCode());
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(IndexingStatus.FAILED.toString());
                site.setName("-");
                siteRep.save(site);
            }
            else{
                //не главная. Сделать запись page
                page.setSiteId(site);
                page.setCode(siteResponse.statusCode());
                pageRep.save(page);
            }
            return;
        }

        //ответ от сайта получен
        StringBuilder content = new StringBuilder();

        //при ошибке парсинга:
        try{
            document = siteResponse.parse();
        }catch (IOException e){
            if (deep == 1){
                //Главная. Сделать запись site
                site.setStatus(IndexingStatus.FAILED.toString());
                site.setUrl(pageUrl);
                site.setName("-");
                site.setLastError("Ошибка парсинга. Не удалось получить Jsoup.nodes.Document. Ошибка: " + e.getMessage());
                siteRep.save(site);
            }
            else {
                //не главная.
                site.setStatusTime(LocalDateTime.now());
                site.setLastError("Ошибка парсинга чтраницы \"" + pageUrl + "\". Не удалось получить Jsoup.nodes.Document." +
                        " Ошибка: " + e.getMessage());
                siteRep.save(site);

                page.setSiteId(site);
                page.setPath(getLocalUrl(pageUrl));
                page.setCode(520);
                page.setContent("-");
                pageRep.save(page);
            }
            return;
        }

        //сайт прочитан и Document получен.
        if (deep == 1 ){
            site.setStatus(IndexingStatus.INDEXING.toString());
            site.setStatusTime(LocalDateTime.now());
            site.setUrl(pageUrl);
            site.setName( getSiteName(document) );
            siteRep.save(site);
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
            if (item.attribute(("href"))==null){
                continue;
            }
            String link = item.attribute("href").getValue();
            //поскольку многопоточность, то нужно при проверке уникальности ссылки тут же ее добавить. синхронизированно.
            link = getFullLink(link);
            if (!isCorrectLink(link) || isSubdomain(link)){
                continue;
            }
            if (linksSet.add( link )){
                linkOnPage.add( link );
            }
        }

        //сохранить собранную о странице информацию
        page.setPath(getLocalUrl(getLocalUrl(pageUrl)));
        page.setContent(content.toString());
        page.setSiteId(site);
        page.setCode(siteResponse.statusCode());
        try {
            System.out.println("Проверка наличия " + page.getPath() + " " + pageRep.existUrlWithSite(site.getId(), page.getPath())  );
            pageRep.save(page);
        }catch (Exception e){
            System.out.println("Ошибка сохранения " + page.getPath() + " ");
        }


        //если не достигли "предельной глубины сканирования" то переходим по каждой ссылке и "читаем" очередную страницу.
        if (deep < deepLimit){
            for (String link : linkOnPage){
                PageParser pageParser = new PageParser(link, linksSet, pool,
                        siteRep, pageRep, lemmaRep , indexRep, deep + 1, deepLimit, site, timeout, readSubDomain);
                System.out.println(">>>>>>>>>>> в работу отправлена ссылка " + link);
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
            //return getProtocol(site.getUrl()) + url;
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

        if (!rez.endsWith("/")){
            return rez.concat("/");
        }else {
            return rez;
        }
    }

    //является ли поддоменом (домен- адрес сайта)
    private boolean isSubdomain(String url){
        if (readSubDomain) {
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
        return Jsoup.connect(pageUrl).execute();
    }
}
