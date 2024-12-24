package searchengine.mechanics;

import lombok.Data;

@Data
public class PageRelevance {
    String uri;
    String title;
    String snipped;
    float relevance;
}
