package com.shanaurin.jobparser.threading;

public class LoggerThread extends Thread {
    private final int maxCount;

    public LoggerThread(String name, int maxCount) {
        super(name);
        this.maxCount = maxCount;
    }

    @Override
    public void run() {
        for (int i = 1; i <= maxCount; i++) {
            System.out.println("Поток: " + getName() +
                    ", порядковый номер: " + i);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}