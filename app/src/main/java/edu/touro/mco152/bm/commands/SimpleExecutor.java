package edu.touro.mco152.bm.commands;

import edu.touro.mco152.bm.persist.DiskRun;

import java.util.ArrayList;
import java.util.List;

/**
 * A serial, single-threaded implementation of {@link BenchmarkExecutor} that
 * also acts as the Observer Pattern Subject.
 *
 * <p>Runs each {@link BenchmarkCommand} synchronously on the calling thread.
 * After a command completes successfully, notifies all registered
 * {@link BenchmarkObserver}s with the completed {@link DiskRun}, so that
 * post-benchmark activities (DB persistence, UI updates, alerting) are
 * decoupled from the commands themselves.
 */
public class SimpleExecutor implements BenchmarkExecutor {

    private final List<BenchmarkObserver> observers = new ArrayList<>();

    /**
     * Registers an observer to be notified after each successful benchmark command.
     *
     * @param observer the observer to add
     */
    public void addObserver(BenchmarkObserver observer) {
        observers.add(observer);
    }

    /**
     * Runs the given command immediately on the calling thread, then notifies
     * all registered observers if the command returned a non-null {@link DiskRun}.
     *
     * @param command the benchmark job to execute
     * @return {@code true} if the command completed successfully (non-null run),
     *         {@code false} if the command returned {@code null} (aborted/error)
     */
    @Override
    public boolean runCommand(BenchmarkCommand command) {
        DiskRun run = command.execute();
        if (run != null) {
            for (BenchmarkObserver observer : observers) {
                observer.onBenchmarkComplete(run);
            }
        }
        return run != null;
    }
}
