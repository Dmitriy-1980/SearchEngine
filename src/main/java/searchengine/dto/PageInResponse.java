package searchengine.dto;

import lombok.Data;

/**Класс возвращаемого объекта.
 */
@Data
public class PageInResponse {
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;
    private String site;
    private String siteName;
}
