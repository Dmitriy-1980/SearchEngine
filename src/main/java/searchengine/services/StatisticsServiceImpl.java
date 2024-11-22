package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Config;
import searchengine.config.Site;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRep;
    private final LemmaRepository lemmaRep;
    private final PageRepository pageRep;
    private final Config config;
    private final SiteServiceImpl siteService;

    //детальная инфа по сайту
    @Override
    public DetailedStatisticsItem getDetailedStatisticsItem(String siteUrl) {
        DetailedStatisticsItem detailedStatisticsItem =new DetailedStatisticsItem();
        if (!siteRep.existUrl(siteUrl)){ //сайта такого в БД нет
            detailedStatisticsItem.setError("Такой сайт еще не индексировался.");
            detailedStatisticsItem.setStatus(IndexingStatus.FAILED.toString());
            detailedStatisticsItem.setUrl(siteUrl);
            detailedStatisticsItem.setLemmas(0);
            detailedStatisticsItem.setPages(0);
            detailedStatisticsItem.setStatusTime(System.currentTimeMillis());
            return detailedStatisticsItem;
        }
        SiteEntity site = siteRep.findByUrl(siteUrl);
        int id = site.getId();
        int lemmaCount = lemmaRep.getCountBySiteId(id);
        int pageCount = pageRep.getCountBySiteId(id);
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
        totalStatistics.setSites( (int) siteRep.count() );
        totalStatistics.setPages( (int) pageRep.count() );
        totalStatistics.setLemmas( (int) lemmaRep.count() );
        if ( ! siteRep.existIndexing() ){
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
        config.checkDuplicate();
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
                //поле выше не понятно по смыслу. Поставил- если так или иначе закончена индексация то true
                statisticsResponse.setResult( false );
                break;
            }
        }
        return statisticsResponse;
    }



}
