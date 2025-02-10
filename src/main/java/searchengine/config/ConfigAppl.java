package searchengine.config;

import jakarta.annotation.PostConstruct;
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
public class ConfigAppl {
    private int deepLimit;
    private List<Site> sites;
    private String userAgent;
    private String referer;
    private int timeout; //таймаут(ms) между обращениями к сайту
    private int responseWait; //время одижания ответа индексируемой страницы
    private int maxFrequency; //макс колстраниц с искомой леммой. (отсев чрезмерно распространенных)
    private String redissonHost; //расположение сервера Redis

    /**<pre>Проверяет список сайтов на случайное дублирование в application.yml
     *Убирает конечный сшеш в адресе и фрагмент "www." в начале адреса.</pre>*/
    @PostConstruct
    public void checkDuplicate(){
        ArrayList<Site> tmpList = new ArrayList<>();
        for (Site site : sites){
            String s = site.getUrl().replaceAll("/$","")
                    .toLowerCase().replace("://www.","://");
            site.setUrl(s);


            if (!tmpList.contains(site)){
                tmpList.add(site);
            }
        }
        sites.clear();
        sites.addAll(tmpList);
    }

    @Bean
    public LuceneMorphology luceneMorphology() throws IOException {
        return new RussianLuceneMorphology(); //вернет объект для работы с русской морфологией
    }


}
