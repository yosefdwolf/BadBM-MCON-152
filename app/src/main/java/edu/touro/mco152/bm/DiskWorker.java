package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;

import jakarta.persistence.EntityManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.App.*;
import static edu.touro.mco152.bm.DiskMark.MarkType.READ;
import static edu.touro.mco152.bm.DiskMark.MarkType.WRITE;

/**
 * Executes disk benchmarking on whatever thread the caller provides.
 * All UI interactions are delegated to a {@link BenchmarkUI} injected at construction
 * time, so this class has no knowledge of Swing or any other UI framework.
 *
 * <p>Depends on static configuration values set in {@link App}.
 * Call {@link #executeBenchmark()} to run. To cancel, use the mechanism provided
 * by the {@link BenchmarkUI} implementation (e.g. {@code SwingWorker.cancel(true)}
 * for the Swing UI). This class polls {@link BenchmarkUI#isCancelled()} between
 * marks so each UI controls its own cancellation signal.
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

        /*
          init local vars that keep track of benchmarks, and a large read/write buffer
         */
        int wUnitsComplete = 0, rUnitsComplete = 0, unitsComplete;
        int wUnitsTotal = App.writeTest ? numOfBlocks * numOfMarks : 0;
        int rUnitsTotal = App.readTest ? numOfBlocks * numOfMarks : 0;
        int unitsTotal = wUnitsTotal + rUnitsTotal;
        float percentComplete;

        int blockSize = blockSizeKb * KILOBYTE;
        byte[] blockArr = new byte[blockSize];
        for (int b = 0; b < blockArr.length; b++) {
            if (b % 2 == 0) {
                blockArr[b] = (byte) 0xFF;
            }
        }

        DiskMark wMark, rMark;  // declare vars that will point to objects used to pass progress to UI

        if (App.autoReset) {
            App.resetTestData();
        }

        int startFileNum = App.nextMarkNumber;
        boolean success = true;

        if (App.writeTest) {
            DiskRun run = new DiskRun(DiskRun.IOMode.WRITE, App.blockSequence);
            run.setNumMarks(App.numOfMarks);
            run.setNumBlocks(App.numOfBlocks);
            run.setBlockSize(App.blockSizeKb);
            run.setTxSize(App.targetTxSizeKb());
            run.setDiskInfo(Util.getDiskInfo(dataDir));

            // Tell logger and UI to display what we know so far about the Run
            ui.showMessage("disk info: (" + run.getDiskInfo() + ")");

            // Create a test data file using the default file system and config-specified location
            if (!App.multiFile) {
                testFile = new File(dataDir.getAbsolutePath() + File.separator + "testdata.jdm");
            }

            /*
              Begin an outer loop for specified duration (number of 'marks') of benchmark,
              that keeps writing data (in its own loop - for specified # of blocks). Each 'Mark' is timed
              and is reported to the UI for display as each Mark completes.
             */
            for (int m = startFileNum; m < startFileNum + App.numOfMarks && !ui.isCancelled(); m++) {

                if (App.multiFile) {
                    testFile = new File(dataDir.getAbsolutePath()
                            + File.separator + "testdata" + m + ".jdm");
                }
                wMark = new DiskMark(WRITE);    // starting to keep track of a new benchmark
                wMark.setMarkNum(m);
                long startTime = System.nanoTime();
                long totalBytesWrittenInMark = 0;

                String mode = App.writeSyncEnable ? "rwd" : "rw";

                try {
                    try (RandomAccessFile rAccFile = new RandomAccessFile(testFile, mode)) {
                        for (int b = 0; b < numOfBlocks; b++) {
                            if (App.blockSequence == DiskRun.BlockSequence.RANDOM) {
                                int rLoc = Util.randInt(0, numOfBlocks - 1);
                                rAccFile.seek((long) rLoc * blockSize);
                            } else {
                                rAccFile.seek((long) b * blockSize);
                            }
                            rAccFile.write(blockArr, 0, blockSize);
                            totalBytesWrittenInMark += blockSize;
                            wUnitsComplete++;
                            unitsComplete = rUnitsComplete + wUnitsComplete;
                            percentComplete = (float) unitsComplete / (float) unitsTotal * 100f;

                            /*
                              Report to UI what percentage of the entire BM (#Marks * #Blocks) is done.
                             */
                            ui.updateProgress((int) percentComplete);
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }

                /*
                  Compute duration, throughput of this Mark's step of BM
                 */
                long endTime = System.nanoTime();
                long elapsedTimeNs = endTime - startTime;
                double sec = (double) elapsedTimeNs / (double) 1000000000;
                double mbWritten = (double) totalBytesWrittenInMark / (double) MEGABYTE;
                wMark.setBwMbSec(mbWritten / sec);
                ui.showMessage("m:" + m + " write IO is " + wMark.getBwMbSecAsString() + " MB/s     "
                        + "(" + Util.displayString(mbWritten) + "MB written in "
                        + Util.displayString(sec) + " sec)");
                App.updateMetrics(wMark);

                /*
                  Let the UI know the interim result described by the current Mark
                 */
                ui.updateStats(wMark.getBwMbSec(), wMark.getCumAvg(), wMark.getCumMax());

                // Keep track of statistics to be displayed and persisted after all Marks are done.
                run.setRunMax(wMark.getCumMax());
                run.setRunMin(wMark.getCumMin());
                run.setRunAvg(wMark.getCumAvg());
                run.setEndTime(new Date());
            } // END outer loop for specified duration (number of 'marks') for WRITE benchmark

            /*
              Persist info about the Write BM Run (e.g. into Derby Database)
             */
            EntityManager em = EM.getEntityManager();
            em.getTransaction().begin();
            em.persist(run);
            em.getTransaction().commit();
        }

        /*
          Most benchmarking systems will try to do some cleanup in between 2 benchmark operations to
          make it more 'fair'. For example a networking benchmark might close and re-open sockets,
          a memory benchmark might clear or invalidate the Op Systems TLB or other caches, etc.
         */

        // Prompt user to clear disk cache before read test so measurements are valid
        if (App.readTest && App.writeTest && !ui.isCancelled()) {
            if (!ui.confirmReadAfterWrite()) {
                App.nextMarkNumber += App.numOfMarks;
                App.state = App.State.IDLE_STATE;
                if (App.autoRemoveData) {
                    Util.deleteDirectory(dataDir);
                }
                ui.benchmarkComplete(true);
                return true;
            }
        }

        // Same as above, just for Read operations instead of Writes.
        if (App.readTest) {
            DiskRun run = new DiskRun(DiskRun.IOMode.READ, App.blockSequence);
            run.setNumMarks(App.numOfMarks);
            run.setNumBlocks(App.numOfBlocks);
            run.setBlockSize(App.blockSizeKb);
            run.setTxSize(App.targetTxSizeKb());
            run.setDiskInfo(Util.getDiskInfo(dataDir));

            ui.showMessage("disk info: (" + run.getDiskInfo() + ")");

            for (int m = startFileNum; m < startFileNum + App.numOfMarks && !ui.isCancelled(); m++) {

                if (App.multiFile) {
                    testFile = new File(dataDir.getAbsolutePath()
                            + File.separator + "testdata" + m + ".jdm");
                }
                rMark = new DiskMark(READ);  // starting to keep track of a new benchmark
                rMark.setMarkNum(m);
                long startTime = System.nanoTime();
                long totalBytesReadInMark = 0;

                try {
                    try (RandomAccessFile rAccFile = new RandomAccessFile(testFile, "r")) {
                        for (int b = 0; b < numOfBlocks; b++) {
                            if (App.blockSequence == DiskRun.BlockSequence.RANDOM) {
                                int rLoc = Util.randInt(0, numOfBlocks - 1);
                                rAccFile.seek((long) rLoc * blockSize);
                            } else {
                                rAccFile.seek((long) b * blockSize);
                            }
                            rAccFile.readFully(blockArr, 0, blockSize);
                            totalBytesReadInMark += blockSize;
                            rUnitsComplete++;
                            unitsComplete = rUnitsComplete + wUnitsComplete;
                            percentComplete = (float) unitsComplete / (float) unitsTotal * 100f;
                            ui.updateProgress((int) percentComplete);
                        }
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    String emsg = "May not have done Write Benchmarks, so no data available to read."
                            + ex.getMessage();
                    ui.showMessage(emsg);
                    App.nextMarkNumber += App.numOfMarks;
                    App.state = App.State.IDLE_STATE;
                    ui.benchmarkComplete(false);
                    return false;
                }

                long endTime = System.nanoTime();
                long elapsedTimeNs = endTime - startTime;
                double sec = (double) elapsedTimeNs / (double) 1000000000;
                double mbRead = (double) totalBytesReadInMark / (double) MEGABYTE;
                rMark.setBwMbSec(mbRead / sec);
                ui.showMessage("m:" + m + " READ IO is " + rMark.getBwMbSec() + " MB/s    "
                        + "(MBread " + mbRead + " in " + sec + " sec)");
                App.updateMetrics(rMark);

                /*
                  Let the UI know the interim result described by the current Mark
                 */
                ui.updateStats(rMark.getBwMbSec(), rMark.getCumAvg(), rMark.getCumMax());

                run.setRunMax(rMark.getCumMax());
                run.setRunMin(rMark.getCumMin());
                run.setRunAvg(rMark.getCumAvg());
                run.setEndTime(new Date());
            }

            /*
              Persist info about the Read BM Run (e.g. into Derby Database)
             */
            EntityManager em = EM.getEntityManager();
            em.getTransaction().begin();
            em.persist(run);
            em.getTransaction().commit();
        }

        App.nextMarkNumber += App.numOfMarks;
        if (App.autoRemoveData) {
            Util.deleteDirectory(dataDir);
        }
        App.state = App.State.IDLE_STATE;
        ui.benchmarkComplete(success);
        return success;
    }
}
