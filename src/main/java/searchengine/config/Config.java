package searchengine.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
@Configuration
@RequiredArgsConstructor
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
    private int maxFrequency; //макс колстраниц с искомой леммой. (отсев чрезмерно распространенных)

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

    @Bean
    public LuceneMorphology luceneMorphology() throws IOException {
        return new RussianLuceneMorphology(); //вернет объект для работы с русской морфологией
    }

}
