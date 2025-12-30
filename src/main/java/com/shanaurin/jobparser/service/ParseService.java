package com.shanaurin.jobparser.service;

import com.shanaurin.jobparser.logging.LoggingDaemon;
import com.shanaurin.jobparser.metrics.ParserMetrics;
import com.shanaurin.jobparser.model.Vacancy;
import com.shanaurin.jobparser.repository.VacancyRepository;
import com.shanaurin.jobparser.service.client.WebFluxMockHtmlClient;
import io.micrometer.core.instrument.Timer;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

@Service
public class ParseService {

    private static final int BATCH_SIZE = 50;

    private final ExecutorService vacancyExecutor;
    private final WebFluxMockHtmlClient mockHtmlClient;
    private final VacancyParser vacancyParser;
    private final VacancyRepository vacancyRepository;
    private final LoggingDaemon loggingDaemon;
    private final ParserMetrics metrics;

    // batch state (thread-safe via lock)
    private final Object batchLock = new Object();
    private final List<Vacancy> batch = new ArrayList<>();

    public ParseService(ExecutorService vacancyExecutor,
                        WebFluxMockHtmlClient mockHtmlClient,
                        VacancyParser vacancyParser,
                        VacancyRepository vacancyRepository,
                        LoggingDaemon loggingDaemon,
                        ParserMetrics metrics) {
        this.vacancyExecutor = vacancyExecutor;
        this.mockHtmlClient = mockHtmlClient;
        this.vacancyParser = vacancyParser;
        this.vacancyRepository = vacancyRepository;
        this.loggingDaemon = loggingDaemon;
        this.metrics = metrics;
    }

    public void parseUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

        Timer.Sample batchSample = metrics.startBatchTimer();
        CountDownLatch latch = new CountDownLatch(urls.size());

        for (String url : urls) {
            vacancyExecutor.submit(() -> {
                try {
                    processUrl(url);
                } finally {
                    latch.countDown();
                }
            });
        }

        // не блокируем вызывающий поток: ждём completion в отдельной задаче
        vacancyExecutor.submit(() -> {
            try {
                latch.await();
                flushBatch(); // важно: слить остаток < 50
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                metrics.incError("unknown");
            } finally {
                metrics.stopBatchTimer(batchSample);
            }
        });
    }

    private void processUrl(String url) {
        metrics.incProcessed();
        Timer.Sample urlSample = metrics.startUrlTimer();

        try {
            Timer.Sample fetchSample = metrics.startFetchTimer();
            String html;
            try {
                html = mockHtmlClient.fetchHtml(url);
            } finally {
                metrics.stopFetchTimer(fetchSample);
            }

            Timer.Sample parseSample = metrics.startParseTimer();
            Vacancy vacancy;
            try {
                vacancy = vacancyParser.parse(html, url);
            } finally {
                metrics.stopParseTimer(parseSample);
            }

            // Собираем в batch и при необходимости "снимаем" пачку на сохранение
            List<Vacancy> toSave = null;
            synchronized (batchLock) {
                batch.add(vacancy);
                if (batch.size() >= BATCH_SIZE) {
                    toSave = new ArrayList<>(batch);
                    batch.clear();
                }
            }

            if (toSave != null) {
                saveBatch(toSave, url);
            }

        } catch (Exception e) {
            metrics.incError(classifyError(e));
            loggingDaemon.log("Error processing url " + url + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
        } finally {
            metrics.stopUrlTimer(urlSample);
        }
    }

    private void flushBatch() {
        List<Vacancy> toSave;
        synchronized (batchLock) {
            if (batch.isEmpty()) {
                return;
            }
            toSave = new ArrayList<>(batch);
            batch.clear();
        }
        saveBatch(toSave, "flush");
    }

    private void saveBatch(List<Vacancy> toSave, String context) {
        Timer.Sample dbSample = metrics.startDbTimer();
        try {
            vacancyRepository.saveAll(toSave);
        } finally {
            metrics.stopDbTimer(dbSample);
        }

        metrics.incSaved(toSave.size());
        loggingDaemon.log("Saved batch of " + toSave.size() + " vacancies (context: " + context + ")");
    }

    private String classifyError(Exception e) {
        if (e instanceof WebClientResponseException || e instanceof WebClientRequestException) {
            return "http";
        }
        if (e instanceof DataAccessException) {
            return "db";
        }
        if (e instanceof IllegalArgumentException || e instanceof NullPointerException) {
            return "parse";
        }
        return "unknown";
    }
}