package edu.touro.mco152.bm.commands;

/**
 * Command Pattern interface for disk benchmark operations.
 *
 * <p>Each concrete command (e.g. {@link WriteCommand}, {@link ReadCommand}) encapsulates
 * a complete benchmark job — its I/O type, all run parameters, and the UI to report
 * progress to — so that the job can be handed to any {@link BenchmarkExecutor}
 * implementation and executed without the caller needing to know the details.
 *
 * <p>This separation is what allows commands to eventually be queued, re-run,
 * executed concurrently, or replayed in different configurations, as described
 * in the hardware-manufacturer user story.
 */
public interface BenchmarkCommand {

    /**
     * Executes the benchmark job this command represents.
     *
     * @return {@code true} if the benchmark completed successfully,
     *         {@code false} if it was aborted or encountered a fatal error
     */
    boolean execute();
}
