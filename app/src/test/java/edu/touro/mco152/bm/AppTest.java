package edu.touro.mco152.bm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for selected pure-logic methods in {@link App}.
 * <p>
 * Methods under test (at least 4, no Swing or DB dependencies):
 * <ul>
 *   <li>{@link App#targetMarkSizeKb()}</li>
 *   <li>{@link App#targetTxSizeKb()}</li>
 *   <li>{@link App#resetTestData()}</li>
 *   <li>{@link App#updateMetrics(DiskMark)}</li>
 * </ul>
 */
class AppTest {

    @BeforeEach
    void setUp() {
        // Reset all global metrics to defaults before every test
        App.resetTestData();
        App.blockSizeKb  = 512;
        App.numOfBlocks  = 32;
        App.numOfMarks   = 25;
    }

    // -----------------------------------------------------------------------
    // targetMarkSizeKb()
    // -----------------------------------------------------------------------

    /**
     * Right-BICEP: <b>RIGHT</b> – Verifies the correct result for a standard,
     * known configuration (blockSizeKb=512, numOfBlocks=32 → 16 384 KB).
     */
    @Test
    void targetMarkSizeKb_standardConfig_returnsCorrectProduct() {
        assertEquals(512L * 32, App.targetMarkSizeKb());
    }

    /**
     * Right-BICEP: <b>BOUNDARY</b>
     * <p>
     * CORRECT Boundary type: <b>Range</b> – Tests the lowest meaningful input
     * values (blockSizeKb=1, numOfBlocks=1).  The result must equal 1 and must
     * not be zero or negative, confirming the function handles the minimum of
     * the valid range.
     */
    @Test
    void targetMarkSizeKb_minimumInputs_returnsOne() {
        App.blockSizeKb = 1;
        App.numOfBlocks = 1;
        assertEquals(1L, App.targetMarkSizeKb());
    }

    /**
     * Right-BICEP: <b>BOUNDARY</b> + <b>RIGHT</b> (via @ParameterizedTest)
     * <p>
     * CORRECT Boundary type: <b>Range</b> – Sweeps across several distinct
     * valid (blockSizeKb, numOfBlocks) pairs to confirm that the multiplication
     * is always correct across the practical range of inputs.
     *
     * @param blockSizeKb  block size in KB
     * @param numOfBlocks  number of blocks
     * @param expectedKb   expected mark size in KB
     */
    @ParameterizedTest
    @CsvSource({
            "64,    4,    256",
            "128,   8,   1024",
            "512,  32,  16384",
            "1024, 16,  16384",
            "1,    1,       1"
    })
    void targetMarkSizeKb_variousConfigs_alwaysReturnsBlockSizeTimesBlocks(
            int blockSizeKb, int numOfBlocks, long expectedKb) {
        App.blockSizeKb = blockSizeKb;
        App.numOfBlocks = numOfBlocks;
        assertEquals(expectedKb, App.targetMarkSizeKb());
    }

    // -----------------------------------------------------------------------
    // targetTxSizeKb()
    // -----------------------------------------------------------------------

    /**
     * Right-BICEP: <b>CROSS-CHECK</b> – Verifies {@link App#targetTxSizeKb()}
     * against an independently computed value: {@code numOfMarks × targetMarkSizeKb()}.
     * Both expressions represent the same quantity by definition; if the
     * implementations diverge the test fails.
     */
    @Test
    void targetTxSizeKb_equalsNumOfMarksTimesTargetMarkSizeKb() {
        long crossCheck = (long) App.numOfMarks * App.targetMarkSizeKb();
        assertEquals(crossCheck, App.targetTxSizeKb(),
                "targetTxSizeKb must equal numOfMarks * targetMarkSizeKb");
    }

    /**
     * Right-BICEP: <b>PERFORMANCE</b> – Confirms that {@link App#targetTxSizeKb()}
     * is fast enough for interactive use.  10 000 calls to a simple arithmetic
     * method must complete well within 100 ms on any modern JVM.
     */
    @Test
    void targetTxSizeKb_repeatedCalls_completeInUnder100ms() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10_000; i++) {
            App.targetTxSizeKb();
        }
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 100,
                "10 000 calls to targetTxSizeKb() should finish in < 100 ms, took: " + elapsed + " ms");
    }

    // -----------------------------------------------------------------------
    // resetTestData()
    // -----------------------------------------------------------------------

    /**
     * Right-BICEP: <b>RIGHT</b>
     * <p>
     * CORRECT Boundary type: <b>Existence</b> – After {@link App#resetTestData()}
     * all metric fields must hold the sentinel value {@code -1} (i.e. the
     * "no data yet" state must exist / be established).  This verifies that
     * the method reliably initialises every field it is responsible for.
     */
    @Test
    void resetTestData_afterPopulatingMetrics_resetsAllToSentinel() {
        // Populate every metric field with non-sentinel values
        App.wMax = 200.0;
        App.wMin =  10.0;
        App.wAvg = 100.0;
        App.rMax = 300.0;
        App.rMin =  20.0;
        App.rAvg = 150.0;
        App.nextMarkNumber = 7;

        App.resetTestData();

        assertEquals(-1, App.wMax, "wMax should be reset to -1");
        assertEquals(-1, App.wMin, "wMin should be reset to -1");
        assertEquals(-1, App.wAvg, "wAvg should be reset to -1");
        assertEquals(-1, App.rMax, "rMax should be reset to -1");
        assertEquals(-1, App.rMin, "rMin should be reset to -1");
        assertEquals(-1, App.rAvg, "rAvg should be reset to -1");
        assertEquals(1,  App.nextMarkNumber, "nextMarkNumber should be reset to 1");
    }

    // -----------------------------------------------------------------------
    // updateMetrics(DiskMark)
    // -----------------------------------------------------------------------

    /**
     * Right-BICEP: <b>ERROR</b> – The method must behave correctly when the
     * metric fields hold their initial sentinel value {@code -1} (an edge /
     * error-like condition where no previous data exists).  The first write
     * mark must initialise wMax, wMin, and wAvg to the mark's bandwidth.
     * <p>
     * CORRECT Boundary type: <b>Existence</b> – Confirms that write metrics
     * transition from "non-existent" ({@code -1}) to a concrete value after
     * the very first update.
     */
    @Test
    void updateMetrics_firstWriteMark_initializesAllWriteMetrics() {
        DiskMark mark = new DiskMark(DiskMark.MarkType.WRITE);
        mark.setMarkNum(1);
        mark.setBwMbSec(100.0);

        App.updateMetrics(mark);

        assertEquals(100.0, App.wMax, "wMax should be initialised to first mark's bandwidth");
        assertEquals(100.0, App.wMin, "wMin should be initialised to first mark's bandwidth");
        assertEquals(100.0, App.wAvg, "wAvg should be initialised to first mark's bandwidth");
    }

    /**
     * Right-BICEP: <b>BOUNDARY</b>
     * <p>
     * CORRECT Boundary type: <b>Ordering</b> – After processing several write
     * marks with different bandwidth values the statistics must satisfy the
     * invariant {@code wMax ≥ wAvg ≥ wMin}.  Violating this ordering would
     * indicate a logic error in how the running min/max/avg are maintained.
     */
    @Test
    void updateMetrics_multipleWriteMarks_statisticsAreOrderedCorrectly() {
        double[] speeds = {50.0, 150.0, 100.0};

        for (int i = 0; i < speeds.length; i++) {
            DiskMark mark = new DiskMark(DiskMark.MarkType.WRITE);
            mark.setMarkNum(i + 1);
            mark.setBwMbSec(speeds[i]);
            App.updateMetrics(mark);
        }

        assertEquals(150.0, App.wMax, "wMax should be the maximum speed seen");
        assertEquals(50.0,  App.wMin, "wMin should be the minimum speed seen");
        assertTrue(App.wMax >= App.wAvg, "wMax must be >= wAvg");
        assertTrue(App.wAvg >= App.wMin, "wAvg must be >= wMin");
    }
}
