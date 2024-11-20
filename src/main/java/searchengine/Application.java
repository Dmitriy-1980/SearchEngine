package searchengine;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import searchengine.mechanics.Indexing;

@SpringBootApplication
//@EntityScan("searchengine")

@ComponentScan("searchengine")
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "indexing-settings")
//@Configuration
public class Application {


    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(Application.class, args);
        System.out.println("Application.main.lastPoint");
    }
}
