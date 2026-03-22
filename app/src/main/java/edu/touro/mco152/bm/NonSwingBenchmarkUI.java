package edu.touro.mco152.bm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory BenchmarkUI implementation for testing without Swing.
 * All state is stored in thread-safe atomic fields so tests can inspect
 * progress, messages, stats, and completion after executeBenchmark() returns.
 */
public class NonSwingBenchmarkUI implements BenchmarkUI {

    private final AtomicInteger progress = new AtomicInteger(0);
    private final List<String> messages = new ArrayList<>();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean completedSuccess = new AtomicBoolean(false);
    private volatile double lastMbPerSec = 0.0;
    private volatile double lastCumAvg = 0.0;
    private volatile double lastCumMax = 0.0;

    @Override
    public void updateProgress(int percent) {
        progress.set(percent);
    }

    @Override
    public void showMessage(String message) {
        messages.add(message);
    }

    @Override
    public void benchmarkComplete(boolean success) {
        completedSuccess.set(success);
        completed.set(true);
    }

    /** Always confirms so read tests proceed automatically in tests. */
    @Override
    public boolean confirmReadAfterWrite() {
        return true;
    }

    @Override
    public void updateStats(double mbPerSec, double cumAvg, double cumMax) {
        lastMbPerSec = mbPerSec;
        lastCumAvg = cumAvg;
        lastCumMax = cumMax;
    }

    // --- Accessors for test assertions ---

    public int getProgress() {
        return progress.get();
    }

    public List<String> getMessages() {
        return messages;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public boolean isCompletedSuccess() {
        return completedSuccess.get();
    }

    public double getLastMbPerSec() {
        return lastMbPerSec;
    }

    public double getLastCumAvg() {
        return lastCumAvg;
    }

    public double getLastCumMax() {
        return lastCumMax;
    }
}
