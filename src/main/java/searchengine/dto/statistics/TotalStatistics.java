package searchengine.dto.statistics;

import lombok.Data;

import java.io.Serializable;

@Data
public class TotalStatistics {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;
}
