package edu.touro.mco152.bm.commands;

/**
 * Executor interface for running {@link BenchmarkCommand} instances.
 *
 * <p>Decouples the client ({@link edu.touro.mco152.bm.DiskWorker}) from any specific
 * execution strategy. The first concrete implementation is {@link SimpleExecutor},
 * which runs commands serially on the calling thread. Future implementations could
 * run commands concurrently, in priority order, with retry logic, etc. — all without
 * changing the client code.
 */
public interface BenchmarkExecutor {

    /**
     * Executes the given command according to this executor's strategy.
     *
     * @param command the benchmark job to run
     * @return {@code true} if the command completed successfully, {@code false} otherwise
     */
    boolean runCommand(BenchmarkCommand command);
}
