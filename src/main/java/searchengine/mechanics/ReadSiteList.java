package searchengine.mechanics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "indexing-settings")
public class ReadSiteList {
    private final Config config;
    private final ForkJoinPool pool;


    public void readSiteList(){
        for (Site site : config.getSites()){
            //todo как то бы суметь отправлять в работу сайты выборочно. Но сеттер для бина не работает (((
            ReadSite readSite = new ReadSite(site.getUrl());
        }
    }

}
