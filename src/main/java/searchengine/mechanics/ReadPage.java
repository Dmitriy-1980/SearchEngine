package searchengine.mechanics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.context.properties.ConfigurationProperties;
import searchengine.mechanics.operationResults.ReadPageResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

//чтение страницы
//@NoArgsConstructor
//@Getter
//@Setter
//@ConfigurationProperties(prefix = "indexing-settings")
public class ReadPage implements Runnable{
    private String siteUrl;
    private String pageUrl;
    private int deep; //"глубина" располоджения страницы
    private int deepLimit; //предельная глубина "сканирования" или вложенности страниц
    private ArrayList<String> linksList = new ArrayList<String>();
    private Document document;
    private String message; //сообщение об ошибке
    private HashMap<String, ReadPageResult> map; //коллекция для сбора страниц. Ключ-адрес. Ключ уникальный.
    private ForkJoinPool pool;

    public ReadPage(String siteUrl, String pageUrl , int deepLimit, int deep, HashMap<String, ReadPageResult> map, ForkJoinPool pool){
        this.siteUrl = siteUrl;
        this.pageUrl = pageUrl;
        this.deepLimit = deepLimit;
        this.deep = deep; //уровень вложенности страницы переданного URL
        this.map = map;
        this.pool = pool;
    }


    @Override
    public void run() {
        read();
    }


    /** чтение и разбор страницы */
    private void read(){
        ReadPageResult readPageResult = new ReadPageResult();
        //если ошибка в получении документа по ссылке
        if (!readDocument()){
            readPageResult.setMsgError(message);
            readPageResult.setReaded(false);
        }
        //поиск ссылей на текущей странице
        searchLinks();
        //тут проверка на уникальность. Так как создание объекта для мапы не мгновенное,
        //то сперва "застолбим место" при помощи null.
        for (String link : linksList){
            synchronized (map){
                if (map.containsKey(link)){
                    map.put(link, null);
                }else {
                    continue;
                }
            }
            readPageResult.setDocument(document);
            readPageResult.setLinks(linksList);
            readPageResult.setReaded(true);
            //todo поля word и content заполнить. Нужны соотв методы
            map.put(link, readPageResult);

            //если предельный уровень вложенности еще не достигнут то каждую ссыль
            // точно так же обработать (рекурсивно ту же задачу создать для нее)
            if (deep < deepLimit){
                ReadPage readPage = new ReadPage(siteUrl, link, deepLimit, ++deep, map, pool);
                pool.submit(readPage);
            }

        }

    }

    /** получение структуированного кода страницы */
    private boolean readDocument()  {
        try {
            Thread.sleep(100);
            document = Jsoup.connect(pageUrl).get();
            return true;
        } catch (Exception e){
            System.out.println("ошибка парсинга - " + e.getMessage());
            document = null;
            message = "Ошибка парсинга страницы (" + siteUrl + pageUrl + "). ";
            return false;
        }
    }

    /** получение списка ссылок на странице (удовлетворяющих условиям) */
    private void searchLinks(){
        //System.out.println("поиск в : " + pageUrl);
        Elements links = document.select("a");
        String link;
        for (Element tegA : links) {

            try {
                link = tegA.attribute("href").getValue();
                //link = tegA.attr("href");
            } catch (NullPointerException e){
                //нет атрибута
                continue;
            }

            //если это не ссылка, то игнорируем
            if (!isUrl(link)){
                continue;
            }

            //если такая ссылка есть в "целевой" мапе то игнорируем ее
            //Для синхронизации потребуется вторая проверка, это долго. Перенес проверку в read()
            //if (map.containsKey(link)){
            //    continue;
            //}

            //todo поддомены запрещены??  нет же такого вроде бы требования. Проверить!
            //если ссылка на страницу этого сайта, не субдомен(??), не на эту же страницу и еще не добавлена в список
            //то она достойна добавления
            link = recoveryLink(link);
            if ( thisSite(link) && !isSubDomain(link) && !isInPage(link) && !linksList.contains(link) ) {
                linksList.add(link);
            }
        }
    }

    /**проверка на правильность ссылки*/
    private boolean isUrl(String url){
        if (url.matches("https?://[a-z0-9]{1,}\\.[a-z]{2,3}")) {
            return true;
        }
        if (url.matches("/.+/")) {
            return true;
        }
        if (url.equals("/")) {
            return false;
        }
        return false;
    }

    /**проверка адреса на соотв правилу "не поддомен"*/
    private boolean isSubDomain(String url){
        if (url.matches("^/") ) {    //("/.+/")) {
            return false;
        }
        if ( url.matches("https?://.+\\..+\\..+") ) {
            return true;
        }
        return false;
    }

    /**проверка адреса на соотв правилу "не ссылка на элемент страницы"*/
    private boolean isInPage(String url){
        if ( url.contains("#") ){
            return true;
        }
        return false;
    }

    /**проверка адреса на соотв правилу "страница этого сайта"*/
    private boolean thisSite(String url){
        if (url.matches(siteUrl + ".+") ) {
            return true;
        }
        if (url.matches("/.+")){
            return true;
        }
        return false;
    }

    /**приведение сокращенной(относительной) ссылки к рабочему виду*/
    private String recoveryLink(String link){
        if ( link.matches("^/.+") ){
            return siteUrl + link.substring(1);
        }
        return link;
    }


}
