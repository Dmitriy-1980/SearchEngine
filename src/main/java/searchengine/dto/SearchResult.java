package searchengine.dto;

import lombok.Data;
import java.util.List;

//результат поиска по списку слов
@Data
public class SearchResult{
    private Boolean result;
    private Integer count;
    private List<PageInResponse> data;
    private String error;
}
