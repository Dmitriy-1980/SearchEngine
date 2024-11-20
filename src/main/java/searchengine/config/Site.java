package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class Site {
    private String url;
    private String name;
}
