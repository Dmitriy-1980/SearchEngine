package searchengine.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

//результат поиска по списку слов
@Data
public class SearchResult{
    boolean result;
    int count;
    List<PageOnRequest> data;
    String error;
}
