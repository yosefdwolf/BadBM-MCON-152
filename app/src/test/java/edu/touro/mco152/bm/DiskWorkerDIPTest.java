package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link DiskWorker} can execute a benchmark end-to-end without
 * any Swing components by injecting a {@link NonSwingBenchmarkUI}.
 *
 * <p>Uses the professor's {@link App#setupDefaultAsPerProperties()} to
 * initialise App configuration from the properties file (or defaults) before
 * each test.
 */
class DiskWorkerDIPTest {

    @BeforeEach
    void setup() {
        App.setupDefaultAsPerProperties();
    }

    /**
     * Runs a minimal write-only benchmark (2 marks, 4 blocks, 64 KB) and asserts:
     * <ul>
     *   <li>The benchmark returned {@code true} (completed OK)</li>
     *   <li>The UI was notified of success via {@code benchmarkComplete(true)}</li>
     *   <li>Progress reached 100 %</li>
     *   <li>At least one message was recorded</li>
     *   <li>Cumulative average MB/s is positive</li>
     * </ul>
     */
    @Test
    void writeOnlyBenchmarkCompletesSuccessfully() throws Exception {
        // Small write-only configuration
        App.numOfMarks = 2;
        App.numOfBlocks = 4;
        App.blockSizeKb = 64;
        App.readTest = false;
        App.writeTest = true;
        App.multiFile = false;
        App.autoReset = true;
        App.blockSequence = DiskRun.BlockSequence.SEQUENTIAL;

        NonSwingBenchmarkUI ui = new NonSwingBenchmarkUI();
        DiskWorker worker = new DiskWorker(ui);

        Boolean result = worker.executeBenchmark();

        assertTrue(result, "executeBenchmark() should return true on success");
        assertTrue(ui.isCompletedSuccess(), "benchmarkComplete(true) should have been called");
        assertEquals(100, ui.getProgress(), "Progress should reach 100 %");
        assertFalse(ui.getMessages().isEmpty(), "At least one message should have been recorded");
        assertTrue(ui.getLastCumAvg() > 0, "Cumulative average MB/s should be positive");
    }
}
