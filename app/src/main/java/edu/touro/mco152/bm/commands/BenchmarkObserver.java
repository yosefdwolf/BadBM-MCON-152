package edu.touro.mco152.bm.commands;

import edu.touro.mco152.bm.persist.DiskRun;

/**
 * Observer interface for the benchmark Observer Pattern.
 *
 * <p>Implementations are notified by {@link SimpleExecutor} (the Subject)
 * after each {@link BenchmarkCommand} completes, receiving the completed
 * {@link DiskRun} so they can perform post-benchmark actions such as
 * persisting to the database, updating the UI, or triggering alerts.
 */
public interface BenchmarkObserver {

    /**
     * Called by the Subject after a benchmark command finishes successfully.
     *
     * @param run the completed run with final metrics (never null)
     */
    void onBenchmarkComplete(DiskRun run);
}
