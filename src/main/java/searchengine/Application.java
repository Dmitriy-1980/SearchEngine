package searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import searchengine.config.Config;
import searchengine.config.SitesList;

@SpringBootApplication
//@EntityScan("searchengine")
@ComponentScan("searchengine")
public class Application {

    public static void main(String[] args) {
//        SpringApplication.run(Application.class, args);
        ApplicationContext context = SpringApplication.run(Application.class, args);
        Config config = context.getBean(Config.class);
        SitesList l = new SitesList();
        System.out.println("stop");
    }
}
