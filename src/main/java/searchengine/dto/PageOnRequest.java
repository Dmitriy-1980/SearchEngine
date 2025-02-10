package searchengine.dto;

import lombok.Data;

/**Класс возвращаемого объекта.
 */
@Data
public class PageOnRequest {
    String uri;
    String title;
    String snipped;
    float relevance;
    String site;
    String siteName;
}
