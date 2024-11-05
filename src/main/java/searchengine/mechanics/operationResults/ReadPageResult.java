package searchengine.mechanics.operationResults;

import lombok.Data;
import org.jsoup.nodes.Document;

import java.util.ArrayList;

//класс с результатами прочтения страницы
@Data
public class ReadPageResult {
    Document document; //html
    String content; //значения тегов==контент
    ArrayList<String> words; //контент разбитый на слова Пока непонятно надо ли.
    ArrayList<String> links; //список ссылок
    String msgError; //текст ошибки
    boolean readed; //флаг- прочитана страница или что то пошло не так
}
