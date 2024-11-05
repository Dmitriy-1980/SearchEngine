package searchengine;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import searchengine.mechanics.ReadSite;
import searchengine.mechanics.ReadSiteList;

@SpringBootApplication
//@EntityScan("searchengine")
@ComponentScan("searchengine")
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "indexing-settings")
public class Application {
    //private final ReadSiteList readSiteList;

    public static void main(String[] args) {
//        SpringApplication.run(Application.class, args);
        ApplicationContext context = SpringApplication.run(Application.class, args);
        //readSiteList.readSiteList();
        context.getBean(ReadSiteList.class).readSiteList();//создать бин и запустить метод
        System.out.println("stop");
    }
}
