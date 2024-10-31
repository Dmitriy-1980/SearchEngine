package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class Config {
    private int deepOfSearch;

//    public Connection


}
