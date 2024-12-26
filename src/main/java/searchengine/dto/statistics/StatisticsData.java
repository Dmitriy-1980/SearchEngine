package searchengine.dto.statistics;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class StatisticsData {
    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed;
}
