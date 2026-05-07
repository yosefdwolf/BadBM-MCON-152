package edu.touro.mco152.bm;

import edu.touro.mco152.bm.commands.BenchmarkExecutor;
import edu.touro.mco152.bm.commands.ReadCommand;
import edu.touro.mco152.bm.commands.SimpleExecutor;
import edu.touro.mco152.bm.commands.WriteCommand;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.App.*;

/**
 * Executes disk benchmarking on whatever thread the caller provides.
 * All UI interactions are delegated to a {@link BenchmarkUI} injected at construction
 * time, so this class has no knowledge of Swing or any other UI framework.
 *
 * <p>Depends on static configuration values set in {@link App}.
 * Call {@link #executeBenchmark()} to run. To cancel, use whatever mechanism
 * the {@link BenchmarkUI} implementation exposes. This class polls
 * {@link BenchmarkUI#isCancelled()} between marks so each UI controls its own
 * cancellation signal.
 */
public class DiskWorker {

    private final BenchmarkUI ui;

    public DiskWorker(BenchmarkUI ui) {
        this.ui = ui;
    }

    /**
     * Runs the write and/or read benchmark synchronously on the calling thread.
     *
     * @return true if the benchmark completed without error
     * @throws Exception if an unexpected failure occurs
     */
    public Boolean executeBenchmark() throws Exception {

        /*
          We 'got here' because: 1: End-user clicked 'Start' on the benchmark UI,
          which triggered the start-benchmark event associated with the App::startBenchmark()
          method.  2: startBenchmark() instantiated a DiskWorker (injecting a BenchmarkUI),
          wrapped it in a SwingWorker, and called execute(), causing the SwingWorker to
          eventually call this executeBenchmark() method on a background thread.
         */
        Logger.getLogger(App.class.getName()).log(Level.INFO, "*** New worker thread started ***");
        ui.showMessage("Running readTest " + App.readTest + "   writeTest " + App.writeTest);
        ui.showMessage("num files: " + App.numOfMarks + ", num blks: " + App.numOfBlocks
                + ", blk size (kb): " + App.blockSizeKb + ", blockSequence: " + App.blockSequence);

        if (App.autoReset) {
            App.resetTestData();
        }

        int startFileNum = App.nextMarkNumber;
        boolean success = true;

        BenchmarkExecutor executor = new SimpleExecutor();

        if (App.writeTest) {
            executor.runCommand(new WriteCommand(ui, App.numOfMarks, App.numOfBlocks,
                    App.blockSizeKb, App.blockSequence, startFileNum));
        }

        // Prompt user to clear disk cache before read test so measurements are valid
        if (App.readTest && App.writeTest && !ui.isCancelled()) {
            if (!ui.confirmReadAfterWrite()) {
                App.nextMarkNumber += App.numOfMarks;
                App.state = App.State.IDLE_STATE;
                if (App.autoRemoveData) {
                    Util.deleteDirectory(App.dataDir);
                }
                ui.benchmarkComplete(true);
                return true;
            }
        }

        if (App.readTest) {
            success = executor.runCommand(new ReadCommand(ui, App.numOfMarks, App.numOfBlocks,
                    App.blockSizeKb, App.blockSequence, startFileNum));
        }

        App.nextMarkNumber += App.numOfMarks;
        if (App.autoRemoveData) {
            Util.deleteDirectory(App.dataDir);
        }
        App.state = App.State.IDLE_STATE;
        ui.benchmarkComplete(success);
        return success;
    }
}
