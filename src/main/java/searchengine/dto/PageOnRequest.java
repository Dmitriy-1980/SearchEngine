package searchengine.dto;

import lombok.Data;

import java.io.Serializable;

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
