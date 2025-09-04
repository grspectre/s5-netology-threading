package com.shanaurin.jobparser.threading;

public class CounterWorker implements Runnable {
    private final int maxCount;

    public CounterWorker(int maxCount) {
        this.maxCount = maxCount;
    }

    @Override
    public void run() {
        for (int i = 1; i <= maxCount; i++) {
            System.out.println("Поток: " + Thread.currentThread().getName() +
                    ", порядковый номер: " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}