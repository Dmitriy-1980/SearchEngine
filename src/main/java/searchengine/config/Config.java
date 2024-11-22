package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class Config {
    private int deepLimit;
    private List<Site> sites;
    private int offset;
    private int limit;

    private String userAgent;
    private String referer;
    private int timeout; //таймаут(ms) между обращениями к сайту
    private int responseWait; //время одижания ответа индексируемой страницы
    private boolean readSubDomain; //обрабатывать ли поддомены

    //проверить список сайтов на уникальность
    // (защита от двойного парсинга при неаккуратном вводе в application)
    public void checkDuplicate(){
        ArrayList<Site> tmpList = new ArrayList<>();

        for (Site site : sites){
            if (!site.getUrl().endsWith("/")){
                site.setUrl(site.getUrl() + "/");
            }
            if (!isExistsUrl(tmpList, site.getUrl())){
                tmpList.add(site);
            }
        }

        sites.clear();
        sites.addAll(tmpList);
    }

    //наличие в списке указанного адреса
    private boolean isExistsUrl(ArrayList<Site> list, String url){
        for (Site site : list){
            if (site.getUrl().equals(url)){
                return true;
            }
        }
        return false;
    }

}
