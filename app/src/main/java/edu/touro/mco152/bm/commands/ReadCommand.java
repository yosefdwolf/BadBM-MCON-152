package edu.touro.mco152.bm.commands;

import edu.touro.mco152.bm.App;
import edu.touro.mco152.bm.BenchmarkUI;
import edu.touro.mco152.bm.DiskMark;
import edu.touro.mco152.bm.Util;
import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import static edu.touro.mco152.bm.App.KILOBYTE;
import static edu.touro.mco152.bm.App.MEGABYTE;
import static edu.touro.mco152.bm.DiskMark.MarkType.READ;

/**
 * Command Pattern implementation for the read benchmark.
 *
 * <p>Encapsulates the read I/O loop formerly inside
 * {@link edu.touro.mco152.bm.DiskWorker}, along with all the run parameters
 * needed to execute it. The parameters are supplied at construction time so
 * this command is self-contained and can be handed to any
 * {@link BenchmarkExecutor} without the executor needing to know about
 * {@link edu.touro.mco152.bm.App}.
 */
public class ReadCommand implements BenchmarkCommand {

    private final BenchmarkUI ui;
    private final int numOfMarks;
    private final int numOfBlocks;
    private final int blockSizeKb;
    private final DiskRun.BlockSequence blockSequence;
    private final int startFileNum;

    public ReadCommand(BenchmarkUI ui, int numOfMarks, int numOfBlocks,
                       int blockSizeKb, DiskRun.BlockSequence blockSequence, int startFileNum) {
        this.ui = ui;
        this.numOfMarks = numOfMarks;
        this.numOfBlocks = numOfBlocks;
        this.blockSizeKb = blockSizeKb;
        this.blockSequence = blockSequence;
        this.startFileNum = startFileNum;
    }

    @Override
    public boolean execute() {
        DiskRun run = new DiskRun(DiskRun.IOMode.READ, blockSequence);
        run.setNumMarks(numOfMarks);
        run.setNumBlocks(numOfBlocks);
        run.setBlockSize(blockSizeKb);
        run.setTxSize(App.targetTxSizeKb());
        run.setDiskInfo(Util.getDiskInfo(App.dataDir));

        ui.showMessage("disk info: (" + run.getDiskInfo() + ")");

        int blockSize = blockSizeKb * KILOBYTE;
        byte[] blockArr = new byte[blockSize];

        int rUnitsComplete = 0;
        int rUnitsTotal = numOfBlocks * numOfMarks;

        for (int m = startFileNum; m < startFileNum + numOfMarks && !ui.isCancelled(); m++) {

            if (App.multiFile) {
                App.testFile = new File(App.dataDir.getAbsolutePath()
                        + File.separator + "testdata" + m + ".jdm");
            }

            DiskMark rMark = new DiskMark(READ);
            rMark.setMarkNum(m);
            long startTime = System.nanoTime();
            long totalBytesReadInMark = 0;

            try {
                try (RandomAccessFile rAccFile = new RandomAccessFile(App.testFile, "r")) {
                    for (int b = 0; b < numOfBlocks; b++) {
                        if (blockSequence == DiskRun.BlockSequence.RANDOM) {
                            int rLoc = Util.randInt(0, numOfBlocks - 1);
                            rAccFile.seek((long) rLoc * blockSize);
                        } else {
                            rAccFile.seek((long) b * blockSize);
                        }
                        rAccFile.readFully(blockArr, 0, blockSize);
                        totalBytesReadInMark += blockSize;
                        rUnitsComplete++;
                        ui.updateProgress((int) ((float) rUnitsComplete / (float) rUnitsTotal * 100f));
                    }
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                ui.showMessage("May not have done Write Benchmarks, so no data available to read."
                        + ex.getMessage());
                return false;
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }

            long endTime = System.nanoTime();
            double sec = (double) (endTime - startTime) / 1_000_000_000;
            double mbRead = (double) totalBytesReadInMark / MEGABYTE;
            rMark.setBwMbSec(mbRead / sec);
            ui.showMessage("m:" + m + " READ IO is " + rMark.getBwMbSec() + " MB/s    "
                    + "(MBread " + mbRead + " in " + sec + " sec)");
            App.updateMetrics(rMark);
            ui.addReadMark(rMark);
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

        return true;
    }
}
