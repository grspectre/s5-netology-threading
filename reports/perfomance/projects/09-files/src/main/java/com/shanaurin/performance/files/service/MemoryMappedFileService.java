package com.shanaurin.performance.files.service;

import com.shanaurin.performance.files.model.CurrencyRate;
import com.shanaurin.performance.files.util.MemoryMonitor;
import com.shanaurin.performance.files.util.PerformanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class MemoryMappedFileService {

    private static final String FILE_PATH = "data_memory_mapped.dat";
    private static final int RECORD_SIZE = 128;

    public PerformanceMetrics writeData(List<CurrencyRate> data) {
        PerformanceMetrics metrics = new PerformanceMetrics("Memory-Mapped File - Запись", data.size());
        MemoryMonitor monitor = new MemoryMonitor();
        monitor.start();

        long startTime = System.nanoTime();

        try (RandomAccessFile file = new RandomAccessFile(FILE_PATH, "rw");
             FileChannel channel = file.getChannel()) {

            long fileSize = (long) data.size() * RECORD_SIZE;
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);

            for (int i = 0; i < data.size(); i++) {
                writeRecordToBuffer(buffer, data.get(i));

                if (i % 10000 == 0) {
                    monitor.updatePeak();
                }
            }

            buffer.force(); // Принудительная синхронизация с диском

        } catch (IOException e) {
            log.error("Ошибка при записи данных через Memory-Mapped File", e);
        }

        long endTime = System.nanoTime();
        metrics.setDuration(endTime - startTime);
        metrics.setMemoryUsedBytes(monitor.getMemoryUsed());
        metrics.setPeakMemoryBytes(monitor.getPeakMemory());

        return metrics;
    }

    public PerformanceMetrics readDataSequential(int count) {
        PerformanceMetrics metrics = new PerformanceMetrics("Memory-Mapped File - Последовательное чтение", count);
        MemoryMonitor monitor = new MemoryMonitor();
        monitor.start();

        long startTime = System.nanoTime();
        List<CurrencyRate> result = new ArrayList<>();

        try (RandomAccessFile file = new RandomAccessFile(FILE_PATH, "r");
             FileChannel channel = file.getChannel()) {

            long fileSize = (long) count * RECORD_SIZE;
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);

            for (int i = 0; i < count; i++) {
                CurrencyRate rate = readRecordFromBuffer(buffer);
                result.add(rate);

                if (i % 10000 == 0) {
                    monitor.updatePeak();
                }
            }

        } catch (IOException e) {
            log.error("Ошибка при чтении данных через Memory-Mapped File", e);
        }

        long endTime = System.nanoTime();
        metrics.setDuration(endTime - startTime);
        metrics.setMemoryUsedBytes(monitor.getMemoryUsed());
        metrics.setPeakMemoryBytes(monitor.getPeakMemory());

        return metrics;
    }

    public PerformanceMetrics readDataRandom(int totalRecords, int samplesToRead) {
        PerformanceMetrics metrics = new PerformanceMetrics("Memory-Mapped File - Случайное чтение", samplesToRead);
        MemoryMonitor monitor = new MemoryMonitor();
        monitor.start();

        Random random = new Random(42);
        long startTime = System.nanoTime();
        List<CurrencyRate> result = new ArrayList<>();

        try (RandomAccessFile file = new RandomAccessFile(FILE_PATH, "r");
             FileChannel channel = file.getChannel()) {

            long fileSize = (long) totalRecords * RECORD_SIZE;
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);

            for (int i = 0; i < samplesToRead; i++) {
                int randomIndex = random.nextInt(totalRecords);
                buffer.position(randomIndex * RECORD_SIZE);
                CurrencyRate rate = readRecordFromBuffer(buffer);
                result.add(rate);

                if (i % 1000 == 0) {
                    monitor.updatePeak();
                }
            }

        } catch (IOException e) {
            log.error("Ошибка при случайном чтении данных через Memory-Mapped File", e);
        }

        long endTime = System.nanoTime();
        metrics.setDuration(endTime - startTime);
        metrics.setMemoryUsedBytes(monitor.getMemoryUsed());
        metrics.setPeakMemoryBytes(monitor.getPeakMemory());

        return metrics;
    }

    private void writeRecordToBuffer(MappedByteBuffer buffer, CurrencyRate rate) {
        buffer.putLong(rate.getId());
        putFixedString(buffer, rate.getCurrencyPair(), 20);
        buffer.putDouble(rate.getRate());
        buffer.putDouble(rate.getVolume());
        putFixedString(buffer, rate.getTimestamp().toString(), 64);
    }

    private CurrencyRate readRecordFromBuffer(MappedByteBuffer buffer) {
        long id = buffer.getLong();
        String currencyPair = getFixedString(buffer, 20);
        double rate = buffer.getDouble();
        double volume = buffer.getDouble();
        String timestamp = getFixedString(buffer, 64);

        return new CurrencyRate(id, currencyPair, rate, volume, LocalDateTime.parse(timestamp));
    }

    private void putFixedString(MappedByteBuffer buffer, String str, int length) {
        byte[] bytes = new byte[length];
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(strBytes, 0, bytes, 0, Math.min(strBytes.length, length));
        buffer.put(bytes);
    }

    private String getFixedString(MappedByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8).trim().replace("\0", "");
    }
}
