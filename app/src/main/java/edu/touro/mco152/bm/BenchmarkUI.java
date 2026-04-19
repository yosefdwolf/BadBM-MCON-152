package edu.touro.mco152.bm;

/**
 * Abstraction layer that decouples the benchmark engine from any specific UI framework.
 * DiskWorker calls methods on this interface instead of making direct Swing calls,
 * allowing the benchmark to run under Swing, headless, or any other environment.
 */
public interface BenchmarkUI {

    /**
     * Records a completed write mark and updates any chart or display that plots per-mark data.
     *
     * @param mark the completed write mark
     */
    void addWriteMark(DiskMark mark);

    /**
     * Records a completed read mark and updates any chart or display that plots per-mark data.
     *
     * @param mark the completed read mark
     */
    void addReadMark(DiskMark mark);

    /**
     * Reports overall benchmark progress as a percentage.
     *
     * @param percent 0–100
     */
    void updateProgress(int percent);

    /**
     * Displays an informational or diagnostic message to the user.
     *
     * @param message the message text
     */
    void showMessage(String message);

    /**
     * Called once the benchmark finishes (or fails).
     *
     * @param success true if the benchmark completed without error
     */
    void benchmarkComplete(boolean success);

    /**
     * Prompts the user to clear the disk cache before a read test follows a write test.
     * Implementations may block until the user acknowledges.
     *
     * @return true if the read test should proceed, false to abort it
     */
    boolean confirmReadAfterWrite();

    /**
     * Delivers the latest throughput statistics after each completed mark.
     *
     * @param mbPerSec  throughput for this mark in MB/s
     * @param cumAvg    cumulative average MB/s so far
     * @param cumMax    cumulative maximum MB/s so far
     */
    void updateStats(double mbPerSec, double cumAvg, double cumMax);

    /**
     * Returns true if the user has requested early termination of the benchmark.
     * Each UI implementation is responsible for tracking its own cancel signal
     * using whatever mechanism fits its environment.
     *
     * @return true if cancellation was requested
     */
    boolean isCancelled();
}
