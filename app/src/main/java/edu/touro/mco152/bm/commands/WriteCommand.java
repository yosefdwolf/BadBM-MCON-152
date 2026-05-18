package edu.touro.mco152.bm.commands;

import edu.touro.mco152.bm.App;
import edu.touro.mco152.bm.BenchmarkUI;
import edu.touro.mco152.bm.DiskMark;
import edu.touro.mco152.bm.Util;
import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import static edu.touro.mco152.bm.App.KILOBYTE;
import static edu.touro.mco152.bm.App.MEGABYTE;
import static edu.touro.mco152.bm.DiskMark.MarkType.WRITE;

/**
 * Command Pattern implementation for the write benchmark.
 *
 * <p>Encapsulates the write I/O loop formerly inside
 * {@link edu.touro.mco152.bm.DiskWorker}, along with all the run parameters
 * needed to execute it. The parameters are supplied at construction time so
 * this command is self-contained and can be handed to any
 * {@link BenchmarkExecutor} without the executor needing to know about
 * {@link edu.touro.mco152.bm.App}.
 */
public class WriteCommand implements BenchmarkCommand {

    private final BenchmarkUI ui;
    private final int numOfMarks;
    private final int numOfBlocks;
    private final int blockSizeKb;
    private final DiskRun.BlockSequence blockSequence;
    private final int startFileNum;

    public WriteCommand(BenchmarkUI ui, int numOfMarks, int numOfBlocks,
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
        DiskRun run = new DiskRun(DiskRun.IOMode.WRITE, blockSequence);
        run.setNumMarks(numOfMarks);
        run.setNumBlocks(numOfBlocks);
        run.setBlockSize(blockSizeKb);
        run.setTxSize(App.targetTxSizeKb());
        run.setDiskInfo(Util.getDiskInfo(App.dataDir));

        ui.showMessage("disk info: (" + run.getDiskInfo() + ")");

        if (!App.multiFile) {
            App.testFile = new File(App.dataDir.getAbsolutePath() + File.separator + "testdata.jdm");
        }

        int blockSize = blockSizeKb * KILOBYTE;
        byte[] blockArr = new byte[blockSize];
        for (int b = 0; b < blockArr.length; b++) {
            if (b % 2 == 0) {
                blockArr[b] = (byte) 0xFF;
            }
        }

        int wUnitsComplete = 0;
        int wUnitsTotal = numOfBlocks * numOfMarks;

        for (int m = startFileNum; m < startFileNum + numOfMarks && !ui.isCancelled(); m++) {

            if (App.multiFile) {
                App.testFile = new File(App.dataDir.getAbsolutePath()
                        + File.separator + "testdata" + m + ".jdm");
            }

            DiskMark wMark = new DiskMark(WRITE);
            wMark.setMarkNum(m);
            long startTime = System.nanoTime();
            long totalBytesWrittenInMark = 0;

            String mode = App.writeSyncEnable ? "rwd" : "rw";

            try {
                try (RandomAccessFile rAccFile = new RandomAccessFile(App.testFile, mode)) {
                    for (int b = 0; b < numOfBlocks; b++) {
                        if (blockSequence == DiskRun.BlockSequence.RANDOM) {
                            int rLoc = Util.randInt(0, numOfBlocks - 1);
                            rAccFile.seek((long) rLoc * blockSize);
                        } else {
                            rAccFile.seek((long) b * blockSize);
                        }
                        rAccFile.write(blockArr, 0, blockSize);
                        totalBytesWrittenInMark += blockSize;
                        wUnitsComplete++;
                        ui.updateProgress((int) ((float) wUnitsComplete / (float) wUnitsTotal * 100f));
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }

            long endTime = System.nanoTime();
            double sec = (double) (endTime - startTime) / 1_000_000_000;
            double mbWritten = (double) totalBytesWrittenInMark / MEGABYTE;
            wMark.setBwMbSec(mbWritten / sec);
            ui.showMessage("m:" + m + " write IO is " + wMark.getBwMbSecAsString() + " MB/s     "
                    + "(" + Util.displayString(mbWritten) + "MB written in "
                    + Util.displayString(sec) + " sec)");
            App.updateMetrics(wMark);
            ui.addWriteMark(wMark);
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

        return true;
    }
}
