package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.ConfigAppl;
import searchengine.dto.CommandResult;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.mechanics.Indexing;
import searchengine.mechanics.MyLog;
import searchengine.mechanics.Search;
import searchengine.services.*;

import java.io.IOException;


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
    private final MyLog log = new MyLog();

    //запуск полной индксации всех указанных в конфигурации сайтов
    @GetMapping("/startIndexing")
    public CommandResult startIndexing() {
        log.traceLog("@GetMapping( /startIndexing )", "info");
        return indexing.startFromList();
    }


    @GetMapping("/stopIndexing")
    public CommandResult stopIndexing(){
        log.traceLog("@GetMapping( /stopIndexing )", "info");
        return indexing.stop();
    }

    //добавить сайт для индексации
    @PostMapping(value = "/indexPage")
    private CommandResult indexPage(@RequestParam("url") String url){
        log.traceLog("ApiController.indexPage", "info");
        return indexing.startAdditionalIndexing(url);
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> statistics() {
        log.traceLog("@GetMapping( /statistics )", "info");
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
                                     @RequestParam(name = "site", defaultValue = "") String siteUrl,
                                     @RequestParam(name = "offset" , defaultValue = "0") int offset,
                                     @RequestParam(name = "limit" , defaultValue = "20") int limit ) {
        log.traceLog("@GetMapping( /search ) " + query + "   " + siteUrl, "info");
        try{
            return new ResponseEntity<>(search.search(query, siteUrl, offset, limit), HttpStatus.OK);
        }catch (IOException e){
            CommandResult commandResult = new CommandResult(false, "Непредвиденная ошибка.");
            return new ResponseEntity<>(commandResult, HttpStatus.valueOf(500));
        }
    }



}
