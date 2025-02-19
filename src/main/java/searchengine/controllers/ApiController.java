package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.CommandResult;
import searchengine.mechanics.Indexing;
import searchengine.mechanics.MyLog;
import searchengine.mechanics.Search;
import searchengine.services.*;

import java.io.IOException;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statServ;
    private final Indexing indexing;
    private final Search search;
    private final MyLog log = new MyLog();

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

    @PostMapping(value = "/indexPage")
    private CommandResult indexPage(@RequestParam("url") String url){
        log.traceLog("ApiController.indexPage", "info");
        return indexing.startAdditionalIndexing(url);
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> statistics() {
        log.traceLog("@GetMapping( /statistics )", "info");
        return new ResponseEntity<>(statServ.getStatistics(), HttpStatus.OK);
    }


    @GetMapping("/search")
    private ResponseEntity<?> search(@RequestParam(name = "query", defaultValue = "") String query,
                                     @RequestParam(name = "site", defaultValue = "") String siteUrl,
                                     @RequestParam(name = "offset" , defaultValue = "0") int offset,
                                     @RequestParam(name = "limit" , defaultValue = "20") int limit ) throws IOException{
        log.traceLog("@GetMapping( /search ) " + query + "   " + siteUrl, "info");
        return new ResponseEntity<>(search.search(query, siteUrl, offset, limit), HttpStatus.OK);
    }



}
