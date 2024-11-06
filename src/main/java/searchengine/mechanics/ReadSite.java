package searchengine.mechanics;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchengine.config.Config;
import searchengine.config.Site;
import searchengine.mechanics.operationResults.ReadPageResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

@Setter
@Getter
@Component
@RequiredArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "indexing-settings")
//разбор конкретного, (url), сайта
public class ReadSite {

    @Autowired
    private Config config;
//    private ForkJoinPool pool; //пул потоков для обраотки страниц
    private ExecutorService pool;
    private HashMap<String,ReadPageResult> map;// = new HashMap<>();
    private String url;

//    public ReadSite(String url){
//        this.pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
//        this.map = new HashMap<>();
//        this.url = url;
//        this.config = new Config();
//    }

    public void go(String url){
        System.out.println("ReadSite.go thread: " + Thread.currentThread());
        //this.pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        this.pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.map = new HashMap<>();
        this.url = url;
            readSite(url);
    }


    public void readSite(String url) {
        ReadPage readPage = new ReadPage(
                url,
                url,
                config.getDeepLimit(),
                1,
                map,
                pool
                );
        Future<?> task = pool.submit(readPage);
        int i = 0;
        while (!task.isDone()){
            try {
                Thread.sleep(200);
                i++;
                if (i == 50){
                    System.out.println("отсечка " + i);
                }
            } catch (InterruptedException e){
                System.out.println("Непроснулся поток: " + Thread.currentThread());
            }


        }
        //теперь есть МАП с адресами и прочими данными по страницам сайта
        //todo надо переносить инфу в БД
        System.out.println("ReadSite.readSite.stop");
    }

}

