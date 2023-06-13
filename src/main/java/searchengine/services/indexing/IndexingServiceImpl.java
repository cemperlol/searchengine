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
            siteRepository.updateStatus(siteId, SiteStatus.INDEXED);
        } else {
            siteRepository.updateLastError(siteId, result.getError());
        }

        if (siteRepository.findAll().stream()
                .noneMatch(site -> site.getStatus().equals(SiteStatus.INDEXING))) {
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
        String baseUrl = HttpWorker.getBaseUrl(url);
        Site site = siteRepository.findByUrl(baseUrl);
        if (site == null && configSites.getSites().stream()
                        .noneMatch(s -> HttpWorker.makeUrlWithoutWWW(s.getUrl()).equals(baseUrl))) {
            return IndexingResponseGenerator.siteNotAdded();
        }
        if (site == null) {
            site = saveSite(baseUrl);
        }
        String pageUrl = HttpWorker.getUrlWithoutDomainName(site.getUrl(), HttpWorker.makeUrlWithSlashEnd(url));

        clearTablesBeforeIndexPage(site, pageUrl);
        WebsiteParser task = new WebsiteParser(site, pageUrl);
        WebsiteParser.subscribe(this);

        IndexingStatusResponse response = task.indexPage();
        WebsiteParser.unsubscribe(this);

        return response;
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

        indexRepository.deleteAllInBatch(page.getIndexes());
        List<Lemma> lemmasToRemove = new ArrayList<>();

        site.getLemmas().forEach(lemma -> {
            if (lemma.getFrequency() == 1) {
                lemmasToRemove.add(lemma);
                lemmaRepository.delete(lemma);
            } else {
                lemmaRepository.decrementFrequencyById(lemma.getId());
            }
        });
        lemmasToRemove.forEach(site.getLemmas()::remove);
        site.getPages().remove(page);
        pageRepository.delete(page);
    }

    private Site saveSite(String siteUrl) {
        Site site = new Site();
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));
        site.setUrl(HttpWorker.makeUrlWithoutWWW(siteUrl));
        site.setName(siteUrl);

        return siteRepository.save(site);
    }

    @Override
    public boolean checkPageExistence(Site site, String path) {
        return site.getPages().stream().anyMatch(p -> p.getPath().equals(path));
    }

    @Override
    public synchronized void update(Site site, Page page, Map<String, Integer> lemmasAndFrequency) {
        if (checkPageExistence(site, page.getPath())) return;

        page = pageRepository.save(page);
        site.getPages().add(page);
        saveLemmasAndIndexes(site, page, lemmasAndFrequency);
        siteRepository.updateStatusTime(site.getId());
    }

    private void saveLemmasAndIndexes(Site site, Page page, Map<String, Integer> lemmasAndFrequency) {
        if (lemmasAndFrequency == null) return;

        saveIndexes(page, saveLemmas(site, lemmasAndFrequency.keySet()),
                lemmasAndFrequency.values().stream().toList());
    }

    private List<Lemma> saveLemmas(Site site, Collection<String> lemmaValues) {
        List<Lemma> existingLemmas = lemmaRepository.findBySiteId(site.getId());
        List<Lemma> lemmas = new ArrayList<>();

        lemmaValues.forEach(lemmaValue -> {
            Lemma lemma = existingLemmas.stream()
                    .filter(l -> l.getLemma().equals(lemmaValue))
                    .findFirst().orElse(null);

            if (lemma != null) {
                lemma.setFrequency(lemma.getFrequency() + 1);
            } else {
                lemma = new Lemma();
                lemma.setSite(site);
                lemma.setLemma(lemmaValue);
                lemma.setFrequency(1);
            }

            lemmas.add(lemma);
        });

        return lemmaRepository.saveAll(lemmas);
    }

    private void saveIndexes(Page page, List<Lemma> lemmas, List<Integer> ranks) {
        List<Index> indexes = new ArrayList<>();
        IntStream.range(0, lemmas.size()).forEach(i -> {
            Lemma lemma = lemmas.get(i);
            int rank = ranks.get(i);

            Index index = new Index();
            index.setLemma(lemma);
            index.setPage(page);
            index.setRank(rank);
            indexes.add(index);
        });

        indexRepository.saveAll(indexes);
    }
}
