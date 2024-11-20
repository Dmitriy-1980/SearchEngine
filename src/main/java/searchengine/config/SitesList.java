package searchengine.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {
    private List<Site> sites;
}
