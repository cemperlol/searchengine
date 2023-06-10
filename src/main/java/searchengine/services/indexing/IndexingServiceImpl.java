package searchengine.services.indexing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.ParsingSubscriber;
import searchengine.dto.indexing.IndexingStatusResponse;
import searchengine.utils.parsers.WebsiteParser;
import searchengine.utils.responseGenerators.IndexingResponseGenerator;
import searchengine.utils.workers.HttpWorker;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

@Service
public class IndexingServiceImpl implements IndexingService, ParsingSubscriber {

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    private final SitesList configSites;

    private static final ForkJoinPool POOL = new ForkJoinPool();

    @Autowired
    public IndexingServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                               LemmaRepository lemmaRepository, IndexRepository indexRepository,
                               SitesList configSites) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.configSites = configSites;
    }

    @Override
    public IndexingStatusResponse startIndexing() {
        if (!WebsiteParser.isParsingStopped()) return IndexingResponseGenerator.indexingAlreadyStarted();

        WebsiteParser.setParsingStopped(false);
        WebsiteParser.subscribe(this);
        CompletableFuture.runAsync(this::prepareIndexing);

        return IndexingResponseGenerator.successResponse();
    }

    private void prepareIndexing() {
        clearTablesBeforeStartIndexing();

        List<WebsiteParser> tasks = configSites.getSites().stream()
                .map(s -> {
                    Site site = new Site();
                    site.setStatus(SiteStatus.INDEXING);
                    site.setStatusTime(new Timestamp(System.currentTimeMillis()));
                    site.setUrl(HttpWorker.makeUrlWithoutWWW(s.getUrl()));
                    site.setName(s.getName());
                    siteRepository.save(site);

                    return new WebsiteParser(site, HttpWorker.makeUrlWithSlashEnd(site.getUrl()));
                }).toList();

        tasks.forEach(t -> CompletableFuture.runAsync(() -> processIndexingResult(t)));
    }

    private void processIndexingResult(WebsiteParser task) {
        IndexingStatusResponse result = POOL.invoke(task);

        int siteId = task.getSite().getId();
        if (result.isResult()) {
            siteRepository.updateStatus(SiteStatus.INDEXED, siteId);
        } else {
            siteRepository.updateLastError(siteId, result.getError());
        }

        if (POOL.getActiveThreadCount() == 0 && POOL.isTerminating()) {
            WebsiteParser.setParsingStopped(true);
            WebsiteParser.unsubscribe(this);
        }
    }

    @Override
    public IndexingStatusResponse stopIndexing() {
        if (WebsiteParser.isParsingStopped()) return IndexingResponseGenerator.noIndexingRunning();

        WebsiteParser.setParsingStopped(true);
        WebsiteParser.unsubscribe(this);
        siteRepository.findAll().stream()
                .filter(site -> site.getStatus().equals(SiteStatus.INDEXING))
                .forEach(site -> siteRepository.updateLastError(site.getId(), "User stopped indexing"));

        return IndexingResponseGenerator.successResponse();
    }

    @Override
    public IndexingStatusResponse indexPage(String url) {
        Site site = siteRepository.findByUrl(HttpWorker.getBaseUrl(url)).orElse(null);
        if (site == null) return IndexingResponseGenerator.siteNotAdded();

        CompletableFuture.runAsync(() -> processIndexPage(site, url));

        return IndexingResponseGenerator.successResponse();
    }

    private void processIndexPage(Site site, String url) {
        String pageUrl = HttpWorker.getUrlWithoutDomainName(site.getUrl(), HttpWorker.makeUrlWithSlashEnd(url));

        clearTablesBeforeIndexPage(site, pageUrl);

        siteRepository.updateStatusTime(site.getId());
    }

    private void clearTablesBeforeStartIndexing() {
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }

    private void clearTablesBeforeIndexPage(Site site, String pageUrl) {
        Page page = site.getPages().stream()
                .filter(sitePage -> sitePage.getPath().equals(pageUrl))
                .findFirst().orElse(null);
        if (page == null) return;

        int pageId = page.getId();
        Map<Lemma, Integer> lemmasAndFrequency = new HashMap<>();
        indexRepository.findByPageId(pageId)
                .forEach(i -> lemmasAndFrequency.put(i.getLemma(), (int) i.getRank()));

        indexRepository.deleteByPageId(pageId);
        lemmasAndFrequency.forEach((lemma, value) -> {
            if (lemma.getFrequency() == value) {
                lemmaRepository.delete(lemma);
            } else {
                lemmaRepository.updateFrequencyById(lemma.getId(), lemma.getFrequency() - value);
            }
        });
        pageRepository.deleteById(pageId);
    }

    @Override
    public void update(Site site, Page page, Map<String, Integer> lemmasAndFrequency) {
        if (site.getPages().stream().anyMatch(sitePage -> sitePage.getPath().equals(page.getPath()))) return;
        siteRepository.updateStatusTime(site.getId());
        synchronized (pageRepository) {
            try {
                site.getPages().add(pageRepository.save(page));
            } catch (Exception e) {
                return;
            }
        }
        saveLemmasAndIndexes(site, page, lemmasAndFrequency);
    }

    private void saveLemmasAndIndexes(Site site, Page page, Map<String, Integer> lemmasAndFrequency) {
        if (lemmasAndFrequency == null) return;
        saveIndexes(page, saveLemmas(site, lemmasAndFrequency.keySet()),
                lemmasAndFrequency.values().stream().toList());
    }

    private List<Lemma> saveLemmas(Site site, Collection<String> lemmaValues) {
        List<Lemma> lemmas = new ArrayList<>();

        for (String lemmaValue : lemmaValues) {
            Lemma lemma = site.getLemmas().stream()
                    .filter(siteLemma -> siteLemma.getLemma().equals(lemmaValue))
                    .findFirst().orElse(null);

            if (lemma == null) {
                lemma = new Lemma();
                lemma.setSite(site);
                lemma.setLemma(lemmaValue);
                lemma.setFrequency(1);
            } else {
                lemma.setFrequency(lemma.getFrequency() + 1);
            }

            try {
                lemmaRepository.save(lemma);
            } catch (Exception e) {
                lemmaRepository.updateFrequencyById(lemma.getId(), lemma.getFrequency());
            }

            lemmas.add(lemma);
        }

        site.getLemmas().addAll(lemmas);
        return lemmas;
    }

    private void saveIndexes(Page page, List<Lemma> lemmas, List<Integer> ranks) {
        List<Index> indexes = new ArrayList<>();

        IntStream.range(0, lemmas.size()).forEach(i -> {
            int rank = ranks.get(i);
            Lemma lemma = lemmas.get(i);
            Index index = indexes.stream()
                    .filter(pageIndex -> pageIndex.getPage().getId() == page.getId()
                            && pageIndex.getLemma().getLemma().equals(lemma.getLemma()))
                    .findFirst().orElse(null);

            if (index == null) {
                index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setRank(rank);
            } else {
                index.setRank(index.getRank() + rank);
            }

            try {
                indexRepository.save(index);
            } catch (Exception e) {
                indexRepository.updateRank(index.getId(), index.getRank());
            }

            indexes.add(index);
        });
    }
}
