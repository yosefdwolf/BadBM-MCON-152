package edu.touro.mco152.bm;

/**
 * Swing adapter stub — full implementation added in issue #4.
 */
public class SwingBenchmarkUI implements BenchmarkUI {

    @Override public void updateProgress(int percent) {}
    @Override public void showMessage(String message) {}
    @Override public void benchmarkComplete(boolean success) {}
    @Override public boolean confirmReadAfterWrite() { return true; }
    @Override public void updateStats(double mbPerSec, double cumAvg, double cumMax) {}
}
