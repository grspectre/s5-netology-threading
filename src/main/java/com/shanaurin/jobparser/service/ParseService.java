package com.shanaurin.jobparser.service;

import com.shanaurin.jobparser.logging.LoggingDaemon;
import com.shanaurin.jobparser.metrics.ParserMetrics;
import com.shanaurin.jobparser.model.Vacancy;
import com.shanaurin.jobparser.repository.VacancyRepository;
import com.shanaurin.jobparser.service.client.WebFluxMockHtmlClient;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
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
    private final Tracer tracer;

    private final Object batchLock = new Object();
    private final List<Vacancy> batch = new ArrayList<>();

    public ParseService(ExecutorService vacancyExecutor,
                        WebFluxMockHtmlClient mockHtmlClient,
                        VacancyParser vacancyParser,
                        VacancyRepository vacancyRepository,
                        LoggingDaemon loggingDaemon,
                        ParserMetrics metrics,
                        Tracer tracer) {
        this.vacancyExecutor = vacancyExecutor;
        this.mockHtmlClient = mockHtmlClient;
        this.vacancyParser = vacancyParser;
        this.vacancyRepository = vacancyRepository;
        this.loggingDaemon = loggingDaemon;
        this.metrics = metrics;
        this.tracer = tracer;
    }

    public void parseUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

        Span batchSpan = tracer.nextSpan()
                .name("parseUrls.batch")
                .tag("jobparser.urls.count", String.valueOf(urls.size()))
                .start();

        Timer.Sample batchSample = metrics.startBatchTimer();
        CountDownLatch latch = new CountDownLatch(urls.size());

        try (Tracer.SpanInScope batchScope = tracer.withSpan(batchSpan)) {

            // Захватываем текущий span для передачи в потоки executor'а
            Span parentSpan = tracer.currentSpan();

            for (String url : urls) {
                vacancyExecutor.submit(() -> {
                    // Создаём дочерний span с явной привязкой к parent
                    Span urlSpan = tracer.nextSpan(parentSpan)
                            .name("processUrl.async")
                            .tag("jobparser.url", url)
                            .start();

                    try (Tracer.SpanInScope urlScope = tracer.withSpan(urlSpan)) {
                        processUrl(url);
                    } catch (Exception e) {
                        urlSpan.error(e);
                    } finally {
                        urlSpan.end();
                        latch.countDown();
                    }
                });
            }

            // Ожидание завершения и flush в отдельной задаче
            vacancyExecutor.submit(() -> {
                Span awaitSpan = tracer.nextSpan(parentSpan)
                        .name("parseUrls.awaitAndFlush")
                        .start();

                try (Tracer.SpanInScope awaitScope = tracer.withSpan(awaitSpan)) {
                    latch.await();
                    flushBatch();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    metrics.incError("unknown");
                    awaitSpan.error(e);
                } catch (Exception e) {
                    awaitSpan.error(e);
                    throw e;
                } finally {
                    awaitSpan.end();
                    metrics.stopBatchTimer(batchSample);
                    batchSpan.end();
                }
            });

        } catch (Exception e) {
            batchSpan.error(e);
            metrics.stopBatchTimer(batchSample);
            batchSpan.end();
            throw e;
        }
    }

    private void processUrl(String url) {
        Span span = tracer.nextSpan()
                .name("processUrl")
                .tag("jobparser.url", url)
                .start();

        metrics.incProcessed();
        Timer.Sample urlSample = metrics.startUrlTimer();

        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {

            // --- Fetch HTML ---
            Timer.Sample fetchSample = metrics.startFetchTimer();
            String html;

            Span fetchSpan = tracer.nextSpan()
                    .name("fetchHtml")
                    .tag("jobparser.url", url)
                    .start();

            try (Tracer.SpanInScope fetchScope = tracer.withSpan(fetchSpan)) {
                html = mockHtmlClient.fetchHtml(url);
            } catch (Exception e) {
                fetchSpan.error(e);
                throw e;
            } finally {
                fetchSpan.end();
                metrics.stopFetchTimer(fetchSample);
            }

            // --- Parse HTML ---
            Timer.Sample parseSample = metrics.startParseTimer();
            Vacancy vacancy;

            Span parseSpan = tracer.nextSpan()
                    .name("parseHtml")
                    .tag("jobparser.url", url)
                    .start();

            try (Tracer.SpanInScope parseScope = tracer.withSpan(parseSpan)) {
                vacancy = vacancyParser.parse(html, url);
            } catch (Exception e) {
                parseSpan.error(e);
                throw e;
            } finally {
                parseSpan.end();
                metrics.stopParseTimer(parseSample);
            }

            // --- Batch accumulation ---
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
            loggingDaemon.log("Error processing url " + url + ": " +
                    e.getClass().getSimpleName() + " - " + e.getMessage());
            span.error(e);
        } finally {
            metrics.stopUrlTimer(urlSample);
            span.end();
        }
    }

    private void flushBatch() {
        Span span = tracer.nextSpan()
                .name("flushBatch")
                .start();

        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            List<Vacancy> toSave;

            synchronized (batchLock) {
                if (batch.isEmpty()) {
                    span.tag("jobparser.batch.size", "0");
                    return;
                }
                toSave = new ArrayList<>(batch);
                batch.clear();
            }

            span.tag("jobparser.batch.size", String.valueOf(toSave.size()));
            saveBatch(toSave, "flush");

        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private void saveBatch(List<Vacancy> toSave, String context) {
        Span dbSpan = tracer.nextSpan()
                .name("saveBatch")
                .tag("jobparser.batch.size", String.valueOf(toSave.size()))
                .tag("jobparser.save.context", context)
                .start();

        Timer.Sample dbSample = metrics.startDbTimer();

        try (Tracer.SpanInScope scope = tracer.withSpan(dbSpan)) {
            vacancyRepository.saveAll(toSave);
        } catch (Exception e) {
            dbSpan.error(e);
            throw e;
        } finally {
            metrics.stopDbTimer(dbSample);
            dbSpan.end();
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