package searchengine.mechanics;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchengine.config.Config;
import searchengine.config.Site;
import searchengine.mechanics.operationResults.ReadPageResult;

import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;

@Setter
@Getter
@Component
//@RequiredArgsConstructor
//@AllArgsConstructor
@ConfigurationProperties(prefix = "indexing-settings")
//разбор конкретного, (url), сайта
public class ReadSite {
    @Autowired
    private Config config;
    @Autowired
    private ForkJoinPool pool; //пул потоков для обраотки страниц
    private HashMap<String,ReadPageResult> map;// = new HashMap<>();
    private String url;

    public ReadSite(String url){
        this.map = new HashMap<>();
        this.url = url;

    }

    public void go(){
        for (Site site : config.getSites()){
            readSite(site.getUrl());
        }
    }


    public void readSite(String url){
        ReadPage readPage = new ReadPage(
                url,
                url,
                config.getDeepLimit(),
                1,
                map,
                pool
                );
        pool.submit(readPage);
        //теперь есть МАП с адресами и прочими данными по страницам сайта
        //todo надо переносить инфу в БД
        System.out.println("ReadSite.readSite.stop");
    }

}

