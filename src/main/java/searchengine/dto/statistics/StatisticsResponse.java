package searchengine.dto.statistics;

import lombok.Data;

import java.io.Serializable;

@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
