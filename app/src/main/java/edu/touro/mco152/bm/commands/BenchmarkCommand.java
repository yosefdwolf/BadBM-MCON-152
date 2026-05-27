package edu.touro.mco152.bm.commands;

import edu.touro.mco152.bm.persist.DiskRun;

/**
 * Command Pattern interface for disk benchmark operations.
 *
 * <p>Each concrete command (e.g. {@link WriteCommand}, {@link ReadCommand}) encapsulates
 * a complete benchmark job — its I/O type, all run parameters, and the UI to report
 * progress to — so that the job can be handed to any {@link BenchmarkExecutor}
 * implementation and executed without the caller needing to know the details.
 *
 * <p>Returning the completed {@link DiskRun} instead of a plain boolean lets the
 * executor (Subject) pass it directly to registered {@link BenchmarkObserver}s,
 * keeping all observer notification logic in one place.
 */
public interface BenchmarkCommand {

    /**
     * Executes the benchmark job this command represents.
     *
     * @return the completed {@link DiskRun} on success, or {@code null} if the
     *         benchmark was aborted or encountered a fatal error
     */
    DiskRun execute();
}
