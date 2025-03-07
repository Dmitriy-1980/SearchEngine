package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigAppl;
import searchengine.config.Site;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
import searchengine.services.LemmaService;
import searchengine.services.PageService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteService siteService;
    private final LemmaService lemmaService;
    private final PageService pageService;
    private final ConfigAppl config;

    //детальная инфа по сайту
    @Override
    public DetailedStatisticsItem getDetailedStatisticsItem(String siteUrl) {
        DetailedStatisticsItem detailedStatisticsItem =new DetailedStatisticsItem();
        if (!siteService.existUrl(siteUrl)){ //сайта такого в БД нет
            detailedStatisticsItem.setError("Такой сайт еще не индексировался.");
            detailedStatisticsItem.setStatus(IndexingStatus.FAILED.toString());
            detailedStatisticsItem.setUrl(siteUrl);
            detailedStatisticsItem.setLemmas(0);
            detailedStatisticsItem.setPages(0);
            detailedStatisticsItem.setStatusTime(System.currentTimeMillis());
            return detailedStatisticsItem;
        }
        SiteEntity site = siteService.findByUrl(siteUrl);
        int id = site.getId();
        int lemmaCount = lemmaService.getCountBySiteId(id);
        int pageCount = pageService.getCountBySiteId(id);
        long statusTime = Timestamp.valueOf(site.getStatusTime()).getTime();

        detailedStatisticsItem.setError(site.getLastError());
        detailedStatisticsItem.setLemmas(lemmaCount);
        detailedStatisticsItem.setName(site.getName());
        detailedStatisticsItem.setPages(pageCount);
        detailedStatisticsItem.setStatus(site.getStatus());
        detailedStatisticsItem.setStatusTime(statusTime);
        detailedStatisticsItem.setUrl(site.getUrl());

        return detailedStatisticsItem;
    }

    //общая инфа- сколько пропарсено сайтов, страниц, найдено лемм
    @Override
    public TotalStatistics getTotalStatistics() {
        TotalStatistics totalStatistics = new TotalStatistics();
        totalStatistics.setSites( (int) siteService.count() );
        totalStatistics.setPages( (int) pageService.count() );
        totalStatistics.setLemmas( (int) lemmaService.count() );
        if ( ! siteService.existIndexing() ){
            totalStatistics.setIndexing(false);
        } else{
            totalStatistics.setIndexing(true);
        }
        return totalStatistics;
    }

    //сборная инфа = общая инфа + детальная по каждому сайту
    @Override
    public StatisticsData getStatisticData() {
        StatisticsData statisticsData = new StatisticsData();
        statisticsData.setTotal( getTotalStatistics() );
        List<DetailedStatisticsItem> listDetaled = new ArrayList<>();
        for (Site site : config.getSites()){
            listDetaled.add( getDetailedStatisticsItem(site.getUrl()) );
        }
        statisticsData.setDetailed( listDetaled );
        return statisticsData;
    }

    //сборная инфа + результат (вся получена или нет)
    @Override
    public StatisticsResponse getStatistics() {
        StatisticsResponse statisticsResponse = new StatisticsResponse();
        statisticsResponse.setStatistics( getStatisticData() );
        statisticsResponse.setResult(true);
        for (DetailedStatisticsItem item : statisticsResponse.getStatistics().getDetailed()){
            if ( item.getStatus().equals(IndexingStatus.INDEXING.toString()) ){
                statisticsResponse.setResult( false );
                break;
            }
        }
        return statisticsResponse;
    }



}
