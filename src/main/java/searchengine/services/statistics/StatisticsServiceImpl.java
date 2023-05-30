package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.dao.site.SiteServiceImpl;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteServiceImpl siteServiceImpl;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics totalStatistics = new TotalStatistics();
        totalStatistics.setSites(siteServiceImpl.getConfigSites().size());

        List<DetailedStatisticsItem> detailedStatistics = siteServiceImpl.getConfigSites()
                .stream()
                .map(configSite -> {
                    Site site = siteServiceImpl.getByUrl(configSite.getUrl());
                    DetailedStatisticsItem item = site == null ? configureItem(configSite) : configureItem(site);

                    totalStatistics.setPages(totalStatistics.getPages() + item.getPages());
                    totalStatistics.setLemmas(totalStatistics.getLemmas() + item.getLemmas());

                    return item;
        }).toList();

        StatisticsData statisticsData = configureStatisticsData(totalStatistics, detailedStatistics);

        return configureStatisticsResponse(statisticsData);
    }

    private DetailedStatisticsItem configureItem(searchengine.config.Site configSite) {
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setName(configSite.getName());
        item.setUrl(configSite.getUrl());
        item.setPages(0);
        item.setLemmas(0);
        item.setStatus(SiteStatus.INDEXING);
        item.setError("Site has not been indexed yet");
        item.setStatusTime(System.currentTimeMillis());

        return item;
    }

    private DetailedStatisticsItem configureItem(Site site) {
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setName(site.getName());
        item.setUrl(site.getUrl());
        item.setPages(site.getPages().size());
        item.setLemmas(site.getLemmas().size());
        item.setStatus(site.getStatus());
        item.setError(site.getLastError() == null ? "" : site.getLastError());
        item.setStatusTime(site.getStatusTime().getTime());

        return item;
    }

    private StatisticsData configureStatisticsData(TotalStatistics totalStatistics,
                                                   List<DetailedStatisticsItem> detailedStatistics) {
        StatisticsData statisticsData = new StatisticsData();
        statisticsData.setTotal(totalStatistics);
        statisticsData.setDetailed(detailedStatistics);

        return statisticsData;
    }

    private StatisticsResponse configureStatisticsResponse(StatisticsData statisticsData) {
        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(statisticsData);
        response.setResult(true);

        return response;
    }
}
