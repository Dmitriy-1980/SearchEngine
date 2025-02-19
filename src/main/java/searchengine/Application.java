package searchengine;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import searchengine.mechanics.MyLog;
@SpringBootApplication

@ComponentScan("searchengine")
@RequiredArgsConstructor
public class Application {
    private static final MyLog log = new MyLog();

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(Application.class, args);
        System.out.println("Приложение запущено");
        log.traceLog("Приложение запущено.", "info");
    }
}
