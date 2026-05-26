package edu.touro.mco152.bm;

import edu.touro.mco152.bm.commands.BenchmarkCommand;
import edu.touro.mco152.bm.commands.BenchmarkExecutor;
import edu.touro.mco152.bm.commands.BenchmarkObserver;
import edu.touro.mco152.bm.commands.ReadCommand;
import edu.touro.mco152.bm.commands.SimpleExecutor;
import edu.touro.mco152.bm.commands.WriteCommand;
import edu.touro.mco152.bm.persist.DiskRun;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Command Pattern implementation directly — no Swing, no DiskWorker.
 *
 * <p>This test class acts as the 'client' described in the Command Pattern:
 * it constructs a {@link SimpleExecutor} and submits {@link WriteCommand} and
 * {@link ReadCommand} instances to it directly, verifying that the commands
 * execute correctly without any involvement of {@link DiskWorker} or the
 * Swing UI framework.
 *
 * <p>Parameters are hardcoded per the assignment specification and are NOT
 * read from {@link App}.
 */
class SimpleExecutorTest {

    private static boolean observerWasCalled = false;

    private static class TestObserver implements BenchmarkObserver {
        @Override
        public void onBenchmarkComplete(DiskRun run) {
            observerWasCalled = true;
        }
    }

    @AfterAll
    static void verifyObserverWasInvoked() {
        assertTrue(observerWasCalled, "BenchmarkObserver should have been notified");
    }

    // Assignment-specified constants — must not be taken from App
    private static final int NUM_MARKS = 25;
    private static final int NUM_BLOCKS = 128;
    private static final int BLOCK_SIZE_KB = 2048;
    private static final DiskRun.BlockSequence BLOCK_SEQUENCE = DiskRun.BlockSequence.SEQUENTIAL;

    @BeforeEach
    void setup() {
        App.setupDefaultAsPerProperties();
        App.multiFile = false;
        App.autoReset = true;
    }

    /**
     * Verifies that a WriteCommand submitted to a SimpleExecutor completes
     * successfully and reports correct progress and throughput — without
     * using DiskWorker or any Swing component.
     */
    @Test
    void writeBenchmarkCompletesSuccessfully() {
        NonSwingBenchmarkUI ui = new NonSwingBenchmarkUI();
        SimpleExecutor executor = new SimpleExecutor();
        executor.addObserver(new TestObserver());
        BenchmarkCommand write = new WriteCommand(
                ui, NUM_MARKS, NUM_BLOCKS, BLOCK_SIZE_KB, BLOCK_SEQUENCE, App.nextMarkNumber);

        boolean result = executor.runCommand(write);

        assertTrue(result, "WriteCommand should return true on success");
        assertFalse(ui.getMessages().isEmpty(), "At least one message should have been recorded");
        assertEquals(100, ui.getProgress(), "Progress should reach 100%");
        assertTrue(ui.getLastCumAvg() > 0, "Cumulative average MB/s should be positive");
        assertTrue(ui.getLastCumMax() > 0, "Cumulative max MB/s should be positive");
    }

    /**
     * Verifies that a ReadCommand submitted to a SimpleExecutor completes
     * successfully after data has been written — without using DiskWorker
     * or any Swing component.
     *
     * <p>A WriteCommand is run first as test setup to ensure data files
     * exist on disk before the read is attempted.
     */
    @Test
    void readBenchmarkCompletesSuccessfully() {
        SimpleExecutor executor = new SimpleExecutor();
        executor.addObserver(new TestObserver());

        // Setup: write data so there is something to read
        NonSwingBenchmarkUI writeUi = new NonSwingBenchmarkUI();
        executor.runCommand(new WriteCommand(
                writeUi, NUM_MARKS, NUM_BLOCKS, BLOCK_SIZE_KB, BLOCK_SEQUENCE, App.nextMarkNumber));

        // Execute the read command and assert on its own UI
        NonSwingBenchmarkUI readUi = new NonSwingBenchmarkUI();
        BenchmarkCommand read = new ReadCommand(
                readUi, NUM_MARKS, NUM_BLOCKS, BLOCK_SIZE_KB, BLOCK_SEQUENCE, App.nextMarkNumber);

        boolean result = executor.runCommand(read);

        assertTrue(result, "ReadCommand should return true on success");
        assertFalse(readUi.getMessages().isEmpty(), "At least one message should have been recorded");
        assertEquals(100, readUi.getProgress(), "Progress should reach 100%");
        assertTrue(readUi.getLastCumAvg() > 0, "Cumulative average MB/s should be positive");
        assertTrue(readUi.getLastCumMax() > 0, "Cumulative max MB/s should be positive");
    }
}
