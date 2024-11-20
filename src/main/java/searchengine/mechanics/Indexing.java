package searchengine.mechanics;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.config.Config;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@RequiredArgsConstructor
public class Indexing {
    private final Config config;
    private final SiteRepository siteRep;
    private final PageRepository pageRep;
    private final LemmaRepository lemmaRep;
    private final IndexRepository indexRep;


    private boolean isRunning = false;
    private HashSet<String> linksSet;
    private ForkJoinPool pool;


    //запустить индексацию
    public boolean start(){
        if (isRunning){
            return false;
        }
        pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());


        linksSet = new HashSet<>();
        isRunning = true;
        for (Site site : config.getSites()){
            clearSiteData(site.getUrl());
            PageParser pageParser = new PageParser(site.getUrl(), linksSet,
                    pool,/* connection,*/ siteRep, pageRep ,
                    lemmaRep , indexRep, 1, config.getDeepLimit(), null ,
                    config.getTimeout(), config.isReadSubDomain());
            pool.submit(pageParser);
        }
        System.out.println("start " + pool);
        return true;
    }

    //остановить индексацию
    public boolean stop(){
        if (!isRunning){
            return false;
        }
        pool.shutdownNow();
        isRunning = false;
        System.out.println("stop " + pool);
        return true;
    }

    //удалить все данные указанного сайта
    private void clearSiteData(String url){
        if (siteRep.existUrl(url)){
            int siteId = siteRep.findByUrl(url).getId();
            pageRep.delAllBySiteId( siteId );
            siteRep.deleteById( siteId );
        }
    }


}
