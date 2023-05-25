package searchengine.dto.statistics;

import lombok.Data;
import searchengine.model.SiteStatus;

@Data
public class DetailedStatisticsItem {
    private String url;

    private String name;

    private SiteStatus status;

    private long statusTime;

    private String error;

    private int pages;

    private int lemmas;
}
