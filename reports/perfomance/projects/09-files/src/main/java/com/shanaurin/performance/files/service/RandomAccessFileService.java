package com.shanaurin.performance.files.service;

import com.shanaurin.performance.files.model.CurrencyRate;
import com.shanaurin.performance.files.util.MemoryMonitor;
import com.shanaurin.performance.files.util.PerformanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class RandomAccessFileService {

    private static final String FILE_PATH = "data_random_access.dat";
    private static final int RECORD_SIZE = 128; // Фиксированный размер записи

    public PerformanceMetrics writeData(List<CurrencyRate> data) {
        PerformanceMetrics metrics = new PerformanceMetrics("RandomAccessFile - Запись", data.size());
        MemoryMonitor monitor = new MemoryMonitor();
        monitor.start();

        long startTime = System.nanoTime();

        try (RandomAccessFile raf = new RandomAccessFile(FILE_PATH, "rw")) {
            for (CurrencyRate rate : data) {
                writeRecord(raf, rate);
                if (data.indexOf(rate) % 10000 == 0) {
                    monitor.updatePeak();
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при записи данных через RandomAccessFile", e);
        }

        long endTime = System.nanoTime();
        metrics.setDuration(endTime - startTime);
        metrics.setMemoryUsedBytes(monitor.getMemoryUsed());
        metrics.setPeakMemoryBytes(monitor.getPeakMemory());

        return metrics;
    }

    public PerformanceMetrics readDataSequential(int count) {
        PerformanceMetrics metrics = new PerformanceMetrics("RandomAccessFile - Последовательное чтение", count);
        MemoryMonitor monitor = new MemoryMonitor();
        monitor.start();

        long startTime = System.nanoTime();
        List<CurrencyRate> result = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(FILE_PATH, "r")) {
            for (int i = 0; i < count; i++) {
                CurrencyRate rate = readRecord(raf, i);
                result.add(rate);
                if (i % 10000 == 0) {
                    monitor.updatePeak();
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при чтении данных через RandomAccessFile", e);
        }

        long endTime = System.nanoTime();
        metrics.setDuration(endTime - startTime);
        metrics.setMemoryUsedBytes(monitor.getMemoryUsed());
        metrics.setPeakMemoryBytes(monitor.getPeakMemory());

        return metrics;
    }

    public PerformanceMetrics readDataRandom(int totalRecords, int samplesToRead) {
        PerformanceMetrics metrics = new PerformanceMetrics("RandomAccessFile - Случайное чтение", samplesToRead);
        MemoryMonitor monitor = new MemoryMonitor();
        monitor.start();

        Random random = new Random(42);
        long startTime = System.nanoTime();
        List<CurrencyRate> result = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(FILE_PATH, "r")) {
            for (int i = 0; i < samplesToRead; i++) {
                int randomIndex = random.nextInt(totalRecords);
                CurrencyRate rate = readRecord(raf, randomIndex);
                result.add(rate);
                if (i % 1000 == 0) {
                    monitor.updatePeak();
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при случайном чтении данных через RandomAccessFile", e);
        }

        long endTime = System.nanoTime();
        metrics.setDuration(endTime - startTime);
        metrics.setMemoryUsedBytes(monitor.getMemoryUsed());
        metrics.setPeakMemoryBytes(monitor.getPeakMemory());

        return metrics;
    }

    private void writeRecord(RandomAccessFile raf, CurrencyRate rate) throws IOException {
        long position = rate.getId() * RECORD_SIZE;
        raf.seek(position);

        raf.writeLong(rate.getId());
        writeFixedString(raf, rate.getCurrencyPair(), 20);
        raf.writeDouble(rate.getRate());
        raf.writeDouble(rate.getVolume());
        writeFixedString(raf, rate.getTimestamp().toString(), 64);
    }

    private CurrencyRate readRecord(RandomAccessFile raf, int index) throws IOException {
        long position = (long) index * RECORD_SIZE;
        raf.seek(position);

        long id = raf.readLong();
        String currencyPair = readFixedString(raf, 20);
        double rate = raf.readDouble();
        double volume = raf.readDouble();
        String timestamp = readFixedString(raf, 64);

        return new CurrencyRate(id, currencyPair, rate, volume, LocalDateTime.parse(timestamp));
    }

    private void writeFixedString(RandomAccessFile raf, String str, int length) throws IOException {
        byte[] bytes = new byte[length];
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(strBytes, 0, bytes, 0, Math.min(strBytes.length, length));
        raf.write(bytes);
    }

    private String readFixedString(RandomAccessFile raf, int length) throws IOException {
        byte[] bytes = new byte[length];
        raf.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8).trim().replace("\0", "");
    }
}
