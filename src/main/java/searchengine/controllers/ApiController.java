package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.mechanics.Indexing;
import searchengine.services.PageService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final PageService pageService;
    private final SiteService siteService;
    private final StatisticsService statServ;
    private final Indexing indexing;


    //запуск полной индксации всех указанных сайтов
    @GetMapping("/startindexing")
    public String startindexing(){
        if ( indexing.start() ) {
            return "{'result': true}";
        } else {
            return "{'result': false, 'error': \"Индексация уже запущена\"";}
        }


    @GetMapping("/stopindexing")
    public String stopIndexing(){
        if (indexing.stop()){
            return "{'result': true}";
        }else {
            return "{'result':false, 'error': \"индексация не запущена\" }";
        }
    }

    //добавить одну, конкретную страницу. URL передается в параметре
    @GetMapping("/indexPage")
    private String indexPage(@RequestBody String url){
        //todo не знаю пока как сделать механизм
        // { 'resulr': true }
        // { 'result': false, "Данная страница находится за пределами сайтов,
        //                         указанных в конфигурационном файле" }
        return null;
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
    private ResponseEntity<?> search(@RequestBody String query) {
        //реализация зависит от вида переденного запроса. SQL строка или иначе
        return null;
    }

}
