package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Config;
import searchengine.config.Site;
import searchengine.dto.CommandResult;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.mechanics.Indexing;
import searchengine.mechanics.Search;
import searchengine.services.*;

import java.io.IOException;
import java.io.Serializable;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final PageService pageService;
    private final SiteService siteService;
    private final StatisticsService statServ;
    private final Indexing indexing;
    private final LuceneService luceneService;
    private final Config config;
    private final Search search;


    //запуск полной индксации всех указанных в конфигурации сайтов
    @GetMapping("/startIndexing")
    public CommandResult startIndexing() {
        CommandResult result = new CommandResult();
        if (indexing.startFromList()) {
            result.setResult(true);
        } else {
            result.setResult(false);
            result.setError("Индексация уже запущена");
        }
        return result;
    }


    @GetMapping("/stopIndexing")
    public CommandResult stopIndexing(){
        CommandResult result = new CommandResult();
        if (indexing.stop()){
            result.setResult(true);
        }else {
            result.setResult(false);
            result.setError("Индексация не запущена");
        }
        return result;
    }

    //добавить сайт для индексации
    @PostMapping(value = "/indexPage")
    private CommandResult indexPage(@RequestParam("url") String url){
        CommandResult result = new CommandResult();
        if (indexing.startAdditionalIndexing(url)){
            result.setResult(true);
        }
        else {
            result.setResult(false);
            result.setError("Индексация уже запущена");
        }
        return result;
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> statistics() {
        try{
            StatisticsResponse stat = statServ.getStatistics();
            ResponseEntity response = new ResponseEntity<>(stat, HttpStatus.OK);
            return response;
        }catch (Exception e){
            e.printStackTrace();
            String errText = "Произошла ошибка при получении статистики.";
            return new ResponseEntity<>(errText, HttpStatus.NOT_FOUND);
        }
    }


    @GetMapping("/search")
    private Serializable search(@RequestParam("query") String query, @RequestParam("site") String siteUrl) {
        System.out.println(query + "   " + siteUrl);

        if (query.isEmpty()){
            return onEmptyQuery();
        }

        if (siteUrl.isEmpty()){
            //запуск для всех сайтов списка
            try {
                search.search(query, "");
                //todo тут ответ- результат поиска
            }catch (IOException e){
                e.printStackTrace();
                return onSearchException();
            }
        }
        else{
            String url = checkUrl(siteUrl);
            if (url.isEmpty()){
                return onAlienSite();
            }else {
                //запуск для конкретного сайта
                System.out.println("one");
            }
        }
        return null;
    }

    //(этот пункт неочевиден) проверить адрес сайта на соответствие формату,
    //принадлежности к индксируемому списку
    //и вернуть или пустую строку""(если не подходит) или url без слеша в конце
    private String checkUrl(String siteUrl){
       String url;
       if (siteUrl.endsWith("/")){
           url = siteUrl.substring(0, siteUrl.length() - 1);
       } else {
           url = siteUrl;
       }

       for (Site site : config.getSites()){
           if (url.equals( site.getUrl() )){
               return url;
           }
       }
       return "";
    }

    //ответ на пустой запрос
    private CommandResult onEmptyQuery(){
        CommandResult response = new CommandResult();
        response.setResult(false);
        response.setError("Задан пустой поисковый запрос");
        return response;
    }

    //сайт не из списка индексации
    private CommandResult onAlienSite(){
        CommandResult response = new CommandResult();
        response.setResult(false);
        response.setError("Заданый сайт не из списка индексации.");
        return response;
    }

    //ошибка при работе поисковика
    private CommandResult onSearchException(){
        CommandResult response = new CommandResult();
        response.setResult(false);
        response.setError("Внутренняя ошибка сервера.");
        return response;
    }
}
