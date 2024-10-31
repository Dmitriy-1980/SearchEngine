package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {




//    private final Random random = new Random();
//    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
//        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
//        String[] errors = {
//                "Ошибка индексации: главная страница сайта не доступна",
//                "Ошибка индексации: сайт не доступен",
//                ""
//        };
//
//
//        /*далее идет блок имитации работы, заполнение dto-шек случайными количествами... */
//        TotalStatistics total = new TotalStatistics();
//        total.setSites(sites.getSites().size());
//        total.setIndexing(true);
//
//        List<DetailedStatisticsItem> detailed = new ArrayList<>();
//        List<Site> sitesList = sites.getSites();   ///нахуя??
//        for(int i = 0; i < sitesList.size(); i++) {
//            Site site = sitesList.get(i);                     //элемент списка = отдельный сайт
//            DetailedStatisticsItem item = new DetailedStatisticsItem(); //детали по сайту
//            item.setName(site.getName());                               //имя его
//            item.setUrl(site.getUrl());                                 //и адрес
//            int pages = random.nextInt(1_000);          //случайное кол страниц <100
//            int lemmas = pages * random.nextInt(1_000); //случайное кол лемм <1000
//            item.setPages(pages);                             //и их устанввливаем в поля
//            item.setLemmas(lemmas);                           //детальной информации
//            item.setStatus(statuses[i % 3]);                  //статус тоже случайно выбираеться из 3 вариантов
//            item.setError(errors[i % 3]);                     //аналогично с ошибкой - случайная.
//            item.setStatusTime(System.currentTimeMillis() -   //время тоже "от балды", но в пределах )))
//                    (random.nextInt(10_000)));
//            total.setPages(total.getPages() + pages);         //добавление кол "пропарсенных" страниц к общему
//            total.setLemmas(total.getLemmas() + lemmas);      //добавление кол "наденных" леммк общему количеству
//            detailed.add(item);                               //добавление "детальной статы" по сайту к общему список "деталек"
//        }
//
//        StatisticsResponse response = new StatisticsResponse();//возвращаемый объект (StatisticData , boolean result)
//        StatisticsData data = new StatisticsData(); //формируем StatisticData объект
//        data.setTotal(total);
//        data.setDetailed(detailed);
//        response.setStatistics(data);               //и заполняем возвращаемый объект
//        response.setResult(true);
//        return response;
        return null;
    }

}
