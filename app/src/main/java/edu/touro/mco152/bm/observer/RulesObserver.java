package edu.touro.mco152.bm.observer;

import edu.touro.mco152.bm.commands.BenchmarkObserver;
import edu.touro.mco152.bm.externalsys.SlackManager;
import edu.touro.mco152.bm.persist.DiskRun;

/**
 * Observer that enforces benchmark quality rules after each run completes.
 * Currently checks: READ benchmarks where runMax exceeds runAvg by more than 3%,
 * which may indicate disk inconsistency worth investigating.
 */
public class RulesObserver implements BenchmarkObserver {

    private final SlackManager slack = new SlackManager("BadBM");

    @Override
    public void onBenchmarkComplete(DiskRun run) {
        if (run.getIoMode() == DiskRun.IOMode.READ
                && run.getRunMax() > run.getRunAvg() * 1.03) {
            slack.postMsg2OurChannel(String.format(
                    ":warning: Read benchmark max (%.2f MB/s) exceeds average (%.2f MB/s) by >3%%",
                    run.getRunMax(), run.getRunAvg()));
        }
    }
}
