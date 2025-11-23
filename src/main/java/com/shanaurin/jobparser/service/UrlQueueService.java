package com.shanaurin.jobparser.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class UrlQueueService {

    private final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();

    public void addAll(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        urlQueue.addAll(urls);
    }

    /**
     * Забирает до maxCount урлов из очереди (не блокируясь, если их меньше).
     */
    public List<String> pollBatch(int maxCount) {
        List<String> result = new ArrayList<>(maxCount);
        urlQueue.drainTo(result, maxCount);
        return result;
    }

    public int size() {
        return urlQueue.size();
    }
}