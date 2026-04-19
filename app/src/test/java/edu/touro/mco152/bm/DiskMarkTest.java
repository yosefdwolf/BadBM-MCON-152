package edu.touro.mco152.bm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for selected methods in {@link DiskMark}.
 * <p>
 * Methods under test:
 * <ul>
 *   <li>{@link DiskMark#getBwMbSecAsString()}</li>
 *   <li>{@link DiskMark#toString()}</li>
 *   <li>{@link DiskMark#setMarkNum(int)} / {@link DiskMark#getMarkNum()}</li>
 * </ul>
 */
class DiskMarkTest {

    // -----------------------------------------------------------------------
    // getBwMbSecAsString()
    // -----------------------------------------------------------------------

    /**
     * Right-BICEP: <b>RIGHT</b>
     * <p>
     * CORRECT Boundary type: <b>Conformance</b> – The string returned by
     * {@link DiskMark#getBwMbSecAsString()} must conform to the
     * {@code DecimalFormat("###.###")} pattern.  A value of 123.456 must be
     * formatted exactly as {@code "123.456"} – no extra leading zeros, no
     * trailing zeros beyond three decimal places, no scientific notation.
     */
    @Test
    void getBwMbSecAsString_standardValue_conformsToDecimalFormat() {
        DiskMark mark = new DiskMark(DiskMark.MarkType.WRITE);
        mark.setBwMbSec(123.456);

        String result = mark.getBwMbSecAsString();

        assertEquals("123.456", result,
                "getBwMbSecAsString() must format 123.456 as '123.456' per the ###.### pattern");
    }

    /**
     * Right-BICEP: <b>BOUNDARY</b>
     * <p>
     * CORRECT Boundary type: <b>Range</b> – Tests the lower boundary of the
     * bandwidth range: {@code 0.0}.  A disk mark with zero throughput (e.g.
     * immediately cancelled) must return {@code "0"} without a decimal point,
     * consistent with the {@code ###.###} format which omits trailing zeros.
     */
    @Test
    void getBwMbSecAsString_zeroBandwidth_returnsZeroString() {
        DiskMark mark = new DiskMark(DiskMark.MarkType.READ);
        mark.setBwMbSec(0.0);

        String result = mark.getBwMbSecAsString();

        assertEquals("0", result,
                "getBwMbSecAsString() should return '0' for a 0.0 bandwidth value");
    }

    // -----------------------------------------------------------------------
    // toString()
    // -----------------------------------------------------------------------

    /**
     * Right-BICEP: <b>RIGHT</b>
     * <p>
     * CORRECT Boundary type: <b>Cardinality</b> – The {@link DiskMark#toString()}
     * result must contain all four expected components: the type label, the mark
     * number, the bandwidth value, and the average value.  Missing any one of
     * these four fields would make the string incomplete (wrong cardinality of
     * information).
     */
    @Test
    void toString_standardWriteMark_containsAllFourExpectedComponents() {
        DiskMark mark = new DiskMark(DiskMark.MarkType.WRITE);
        mark.setMarkNum(5);
        mark.setBwMbSec(75.5);
        mark.setCumAvg(60.0);

        String result = mark.toString();

        assertTrue(result.contains("WRITE"),   "toString must contain the type label 'WRITE'");
        assertTrue(result.contains("5"),        "toString must contain the mark number '5'");
        assertTrue(result.contains("75.5"),     "toString must contain the bandwidth '75.5'");
        assertTrue(result.contains("60"),       "toString must contain the cumulative average '60'");
    }

    // -----------------------------------------------------------------------
    // setMarkNum() / getMarkNum()
    // -----------------------------------------------------------------------

    /**
     * Right-BICEP: <b>RIGHT</b> – Confirms that {@link DiskMark#setMarkNum(int)}
     * stores the value and {@link DiskMark#getMarkNum()} retrieves it unchanged
     * (basic round-trip correctness).
     */
    @Test
    void setGetMarkNum_roundTrip_retrievesSameValue() {
        DiskMark mark = new DiskMark(DiskMark.MarkType.READ);
        mark.setMarkNum(42);

        assertEquals(42, mark.getMarkNum(),
                "getMarkNum() must return the value that was set via setMarkNum()");
    }
}
