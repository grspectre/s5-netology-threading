package com.shanaurin.jobparser.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ParserMetrics {

    private final MeterRegistry registry;

    private final Timer urlTotalTimer;
    private final Timer batchTotalTimer;

    private final Counter urlProcessed;
    private final Counter urlSaved;

    private final Counter urlErrorHttp;
    private final Counter urlErrorParse;
    private final Counter urlErrorDb;
    private final Counter urlErrorUnknown;

    public ParserMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.urlTotalTimer = Timer.builder("jobparser.url.total.time")
                .description("Total processing time per URL (fetch + parse + save)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        this.batchTotalTimer = Timer.builder("jobparser.batch.total.time")
                .description("Total processing time per batch (submission or completion marker)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        this.urlProcessed = Counter.builder("jobparser.url.processed.total")
                .description("How many URLs were attempted to process")
                .register(registry);

        this.urlSaved = Counter.builder("jobparser.vacancy.saved.total")
                .description("How many Vacancy entities were saved")
                .register(registry);

        this.urlErrorHttp = Counter.builder("jobparser.url.errors.total")
                .description("URL processing errors by type")
                .tag("type", "http")
                .register(registry);

        this.urlErrorParse = Counter.builder("jobparser.url.errors.total")
                .description("URL processing errors by type")
                .tag("type", "parse")
                .register(registry);

        this.urlErrorDb = Counter.builder("jobparser.url.errors.total")
                .description("URL processing errors by type")
                .tag("type", "db")
                .register(registry);

        this.urlErrorUnknown = Counter.builder("jobparser.url.errors.total")
                .description("URL processing errors by type")
                .tag("type", "unknown")
                .register(registry);
    }

    public Timer.Sample startUrlTimer() {
        return Timer.start(registry);
    }

    public void stopUrlTimer(Timer.Sample sample) {
        sample.stop(urlTotalTimer);
    }

    public Timer.Sample startBatchTimer() {
        return Timer.start(registry);
    }

    public void stopBatchTimer(Timer.Sample sample) {
        sample.stop(batchTotalTimer);
    }

    public void incProcessed() {
        urlProcessed.increment();
    }

    public void incSaved() {
        urlSaved.increment();
    }

    public void incError(String type) {
        switch (type) {
            case "http" -> urlErrorHttp.increment();
            case "parse" -> urlErrorParse.increment();
            case "db" -> urlErrorDb.increment();
            default -> urlErrorUnknown.increment();
        }
    }
}