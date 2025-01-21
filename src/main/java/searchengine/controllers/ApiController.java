package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.ConfigAppl;
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
    private final ConfigAppl config;
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
//    @GetMapping("/startIndexing")
//    public String startIndexing() throws InterruptedException {
//        int time = 10000;
//        Thread.sleep(time);
//        return "{'result':true}";
//    }


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
    private ResponseEntity<?> search(@RequestParam(name = "query", defaultValue = "") String query,
                                     @RequestParam(name = "site", defaultValue = "") String siteUrl) {
        System.out.println(query + "   " + siteUrl);
        try{
            return new ResponseEntity<>(search.search(query, siteUrl), HttpStatus.OK);
        }catch (IOException e){
            CommandResult commandResult = new CommandResult();
            commandResult.setResult(false);
            commandResult.setError("Непредвиденная ошибка.");
            return new ResponseEntity<>(commandResult, HttpStatus.valueOf(500));
        }
    }



}
