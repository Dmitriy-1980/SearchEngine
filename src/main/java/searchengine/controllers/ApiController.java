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


    //запуск полной индксации всех указанных в конфигурации сайтов
    @GetMapping("/startIndexing")
    public String startIndexing(){
        if ( indexing.startFromList() ) {
            return "{'result': true}";
        } else {
            return "{'result': false, 'error': \"Индексация уже запущена\"";}
        }


    @GetMapping("/stopIndexing")
    public String stopIndexing(){
        if (indexing.stop()){
            return "{'result': true}";
        }else {
            return "{'result':false, 'error': \"Индексация не запущена\" }";
        }
    }

    //добавить сайт для индексации
    @PostMapping(value = "/indexPage")
    private String indexPage(@RequestParam("url") String url){
        if (indexing.startAdditionalIndexing(url)){
            return "{ 'result': true }";
        }
        else {
            return "{ 'result': false, \"Индексация уже запущена\" }";
        }
        //todo пояснение куратора противоречит ТЗ. уточнить
        // по ТЗ воттакой ответпредусмотрен. Отсыда, вроде как, и логика иная прослеживается.
        // { 'result': false, "Данная страница находится за пределами сайтов,
        //                         указанных в конфигурационном файле" }
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
