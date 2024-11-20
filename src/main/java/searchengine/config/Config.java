package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
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

}
