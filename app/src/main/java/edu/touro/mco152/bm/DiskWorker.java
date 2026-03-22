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
 * Call {@link #executeBenchmark()} to run; call {@link #cancel()} from another
 * thread to request early termination.
 */
public class DiskWorker {

    private final BenchmarkUI ui;
    private volatile boolean cancelled = false;

    public DiskWorker(BenchmarkUI ui) {
        this.ui = ui;
    }

    /** Requests that the benchmark stop after the current block completes. */
    public void cancel() {
        cancelled = true;
    }

    private boolean isCancelled() {
        return cancelled;
    }

    /**
     * Runs the write and/or read benchmark synchronously on the calling thread.
     *
     * @return true if the benchmark completed without error
     * @throws Exception if an unexpected failure occurs
     */
    public Boolean executeBenchmark() throws Exception {

        Logger.getLogger(App.class.getName()).log(Level.INFO, "*** New worker thread started ***");
        ui.showMessage("Running readTest " + App.readTest + "   writeTest " + App.writeTest);
        ui.showMessage("num files: " + App.numOfMarks + ", num blks: " + App.numOfBlocks
                + ", blk size (kb): " + App.blockSizeKb + ", blockSequence: " + App.blockSequence);

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

        DiskMark wMark, rMark;

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

            ui.showMessage("disk info: (" + run.getDiskInfo() + ")");

            if (!App.multiFile) {
                testFile = new File(dataDir.getAbsolutePath() + File.separator + "testdata.jdm");
            }

            for (int m = startFileNum; m < startFileNum + App.numOfMarks && !isCancelled(); m++) {

                if (App.multiFile) {
                    testFile = new File(dataDir.getAbsolutePath()
                            + File.separator + "testdata" + m + ".jdm");
                }
                wMark = new DiskMark(WRITE);
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
                            ui.updateProgress((int) percentComplete);
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }

                long endTime = System.nanoTime();
                long elapsedTimeNs = endTime - startTime;
                double sec = (double) elapsedTimeNs / (double) 1000000000;
                double mbWritten = (double) totalBytesWrittenInMark / (double) MEGABYTE;
                wMark.setBwMbSec(mbWritten / sec);
                ui.showMessage("m:" + m + " write IO is " + wMark.getBwMbSecAsString() + " MB/s     "
                        + "(" + Util.displayString(mbWritten) + "MB written in "
                        + Util.displayString(sec) + " sec)");
                App.updateMetrics(wMark);
                ui.updateStats(wMark.getBwMbSec(), wMark.getCumAvg(), wMark.getCumMax());

                run.setRunMax(wMark.getCumMax());
                run.setRunMin(wMark.getCumMin());
                run.setRunAvg(wMark.getCumAvg());
                run.setEndTime(new Date());
            }

            EntityManager em = EM.getEntityManager();
            em.getTransaction().begin();
            em.persist(run);
            em.getTransaction().commit();
        }

        if (App.readTest && App.writeTest && !isCancelled()) {
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

        if (App.readTest) {
            DiskRun run = new DiskRun(DiskRun.IOMode.READ, App.blockSequence);
            run.setNumMarks(App.numOfMarks);
            run.setNumBlocks(App.numOfBlocks);
            run.setBlockSize(App.blockSizeKb);
            run.setTxSize(App.targetTxSizeKb());
            run.setDiskInfo(Util.getDiskInfo(dataDir));

            ui.showMessage("disk info: (" + run.getDiskInfo() + ")");

            for (int m = startFileNum; m < startFileNum + App.numOfMarks && !isCancelled(); m++) {

                if (App.multiFile) {
                    testFile = new File(dataDir.getAbsolutePath()
                            + File.separator + "testdata" + m + ".jdm");
                }
                rMark = new DiskMark(READ);
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
                ui.updateStats(rMark.getBwMbSec(), rMark.getCumAvg(), rMark.getCumMax());

                run.setRunMax(rMark.getCumMax());
                run.setRunMin(rMark.getCumMin());
                run.setRunAvg(rMark.getCumAvg());
                run.setEndTime(new Date());
            }

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
