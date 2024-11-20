package searchengine.services;

import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

public interface StatisticsService {

    //получить детальную статистику по сайту сайту
    DetailedStatisticsItem getDetailedStatisticsItem(String siteUrl);


    //получить общее кол сайтов, страниц, лемм
    TotalStatistics getTotalStatistics();


    //сборная статистическая инфа - общая и по сайтам
    StatisticsData getStatisticData();
    

    //получить сводную статистику
    StatisticsResponse getStatistics();






}
