package com.shanaurin.jobparser.service;

import com.shanaurin.jobparser.threading.CounterWorker;
import com.shanaurin.jobparser.threading.LoggerThread;
import org.springframework.stereotype.Service;

@Service
public class ThreadManagementService {

    public void demonstrateThreads() {
        // Создание и запуск потока через Runnable
        CounterWorker counterWorker = new CounterWorker(5);
        Thread counterThread = new Thread(counterWorker, "CounterWorker");

        // Создание и запуск потока через наследование Thread
        LoggerThread loggerThread = new LoggerThread("LoggerThread", 5);

        // Запуск потоков
        counterThread.start();
        loggerThread.start();

        // Вывод информации о всех активных потоках
        printActiveThreads();

        // Ожидание завершения потоков
        try {
            counterThread.join();
            loggerThread.join();
            System.out.println("Все демонстрационные потоки завершены");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void printActiveThreads() {
        System.out.println("\n=== Активные потоки ===");
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }

        Thread[] threads = new Thread[rootGroup.activeCount()];
        rootGroup.enumerate(threads);

        for (Thread thread : threads) {
            if (thread != null) {
                System.out.println("Поток: " + thread.getName() +
                        ", Статус: " + thread.getState() +
                        ", Демон: " + thread.isDaemon());
            }
        }
        System.out.println("========================\n");
    }
}