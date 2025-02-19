package searchengine.mechanics;

import searchengine.config.Site;

import java.util.List;

public class Utilites {


    /**Получить из полного адреса и адреса сайта локальный адрес страницы.
     * @param fullUrl полный адрес страницы
     * @param siteUrl адрес сайта
     * @return локальный адрес страницы /адрес/ или пустую строку, если
     * символ после адреса сайта не слеш*/
    public static String getLocalUrl(String fullUrl, String siteUrl){
        String pageUrl = fullUrl .substring(siteUrl.length());
        return pageUrl.startsWith("/") ? pageUrl : "";
    }

    /**Получить локальный адрес страницы из полного, если она соотв сайту из переданного списка.
     * @param fullUrl полный адрес страницы
     * @param sitesList список сайтов доступных для индексации
     * @return локальный адрес страницы /адрес/, или пустая строка, если нет такого сайта в списке*/
    public static String getLocalUrl(String fullUrl, List<Site> sitesList){
        String withOutWWW = fullUrl.replace("://www.", "://");
        String siteUrl = pagesSite(withOutWWW,sitesList);
        if (siteUrl.isEmpty()){ return ""; }
        return getLocalUrl(withOutWWW, siteUrl);
    }

    /**Получить url сайта из списка соотв переданному fullUrl.
     *@param fullUrl полный адрес страницы
     * @param sitesList список сайтов
     * @return адрес сайта
     * <b>или пустая строка если такого сайте в списке нет.</b>*/
    public static String pagesSite(String fullUrl, List<Site> sitesList){
        String pageUrl = fullUrl.toLowerCase().replace("://www.","://");
        for (Site site : sitesList) {
            if (pageUrl.startsWith(site.getUrl())  )
                { return site.getUrl(); }
        }
        return "";
    }

    /**Проверяет принадлежит ли ссылка сайту напрямую,
     * то есть не является поддоменом и не ведет на другие ресурсы.
     * @param url проверяемая ссылка
     * @param siteUrl адрес сайта
     * @return true если ссылка этого сайта.*/
    public static boolean inThisSite(String url, String siteUrl){
        String siteWOP = ""; // site WithOut Protocol
        if (siteUrl.startsWith("https://")) { siteWOP = siteUrl.substring(8); }
            else { siteWOP = siteUrl.substring(7); }
        if (url.startsWith(siteUrl) || url.startsWith("/"+siteWOP) || url.startsWith("//"+siteWOP)){
            return true;
        }
        return false;
    }

    /**@param url полный адрес страницы.
     * <pre>Вернет протокол из полного адреса.
     * http: или https: или <> если не они .</></pre>*/
    public static String getProtocol(String url){
        if (url.startsWith("https://")){
            return  "https:";
        } else if (url.startsWith("http://")) {
            return  "http:";
        } else {
            return "<>";
        }
    }


    //проверить ссылку на соответствие требуему виду (
    /**<pre>Проверка ссылки на корректность: Корректная должна начинаться на :
     * / или // или http:// или https://
     * и не должна кончаться на расширение файла</pre>*/
    public static boolean isCorrectLink(String url){
        if (url.matches(".+\\.[a-zA-Z0-9]{1,10}(\\?.*)*$")){ return false; }//отсевссылок на файлы
        if (url.startsWith("/") && url.length() > 1){ return true; }
        if (url.startsWith("//") && url.length() > 5){ return true; }
        if (url.startsWith("http://") && url.length() > 10){ return true; }
        if (url.startsWith("https://") && url.length() > 11){ return true; }
        return false;
    }

    //получить полную ссылку
    /**Из полученой ссылки , адреса сайта и протокола получить полнуюссылку.
     * @param url ссылка
     * @param siteUrl адрес сайта
     * @param protocol протокол
     * @return Полный адрес, если входящая ссылка начинается с / или //.
     * <b>Если НЕ начинается на / или // то вернет входящую ссылку.</b>*/
    public static String getFullUrl(String url, String siteUrl, String protocol){
        if (url.startsWith("//")){ return protocol + url; }
        if (url.startsWith("/")){ return siteUrl + url; }
        return url;
    }

    /**<pre>
     *Проверяет принадлежность сайта списку заданных </pre>
     * @param siteUrl поверяемый адрес
     * @param siteList список заданных адресов
     * @return true если сайт из списка.*/
    public static boolean isExistSite(String siteUrl, List<Site> siteList){
         String s = siteUrl.toLowerCase().replace("://www.","://");
         for (Site site : siteList){
             if (site.getUrl().equals(s)) { return true; }
         }
         return false;
    }
}
