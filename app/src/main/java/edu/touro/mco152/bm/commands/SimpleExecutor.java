package edu.touro.mco152.bm.commands;

/**
 * A serial, single-threaded implementation of {@link BenchmarkExecutor}.
 *
 * <p>Runs each {@link BenchmarkCommand} synchronously on the calling thread,
 * one at a time, in the order submitted. This is the simplest possible execution
 * strategy and serves as the baseline executor for the Command Pattern refactor.
 *
 * <p>Future executors (e.g. concurrent, prioritized, scheduled) can be introduced
 * by implementing {@link BenchmarkExecutor} without touching any client code.
 */
public class SimpleExecutor implements BenchmarkExecutor {

    /**
     * Runs the given command immediately on the calling thread.
     *
     * @param command the benchmark job to execute
     * @return the result returned by {@link BenchmarkCommand#execute()}
     */
    @Override
    public boolean runCommand(BenchmarkCommand command) {
        return command.execute();
    }
}
