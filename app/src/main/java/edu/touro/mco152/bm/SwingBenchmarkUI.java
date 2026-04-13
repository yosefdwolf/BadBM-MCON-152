package edu.touro.mco152.bm;

import edu.touro.mco152.bm.ui.Gui;

import javax.swing.*;

/**
 * Concrete Swing adapter for {@link BenchmarkUI}.
 * Isolates all Swing/JFreeChart calls that were previously scattered through
 * {@link DiskWorker}, concentrating them in one place so the benchmark engine
 * remains framework-agnostic.
 *
 * <p>All methods that touch Swing components dispatch onto the Event Dispatch
 * Thread via {@link SwingUtilities#invokeLater} so they are safe to call from
 * any worker thread.
 */
public class SwingBenchmarkUI implements BenchmarkUI {

    /**
     * Reference to the SwingWorker that is running the benchmark.
     * Set via {@link #setSwingRunner} immediately after the SwingWorker is created,
     * so that {@link #isCancelled()} can delegate to its built-in cancel signal
     * rather than requiring a separate cancel flag in DiskWorker.
     */
    private SwingWorker<?, ?> swingRunner;

    /** Wires in the SwingWorker so {@link #isCancelled()} can delegate to it. */
    public void setSwingRunner(SwingWorker<?, ?> runner) {
        this.swingRunner = runner;
    }

    public SwingBenchmarkUI() {
        // Initialise chart legend visibility from current App config.
        // If autoReset is on, clear chart series and reset metrics labels.
        Gui.updateLegend();
        if (App.autoReset) {
            Gui.resetTestData();
        }
    }

    /**
     * Updates the progress bar value and its KB-processed label.
     */
    @Override
    public void updateProgress(int percent) {
        SwingUtilities.invokeLater(() -> {
            Gui.progressBar.setValue(percent);
            long kbProcessed = (long) percent * App.targetTxSizeKb() / 100;
            Gui.progressBar.setString(kbProcessed + " / " + App.targetTxSizeKb());
        });
    }

    /**
     * Appends a message to the main frame's message area.
     */
    @Override
    public void showMessage(String message) {
        SwingUtilities.invokeLater(() -> Gui.mainFrame.msg(message));
    }

    /**
     * Re-enables UI controls and refreshes the run history panel from the database.
     */
    @Override
    public void benchmarkComplete(boolean success) {
        SwingUtilities.invokeLater(() -> {
            App.loadSavedRuns();
            Gui.mainFrame.adjustSensitivity();
        });
    }

    /**
     * Shows the disk-cache-clear advisory that was previously a blocking
     * {@code JOptionPane.showMessageDialog} inside DiskWorker.
     * Converted to a confirm dialog so the user can optionally cancel the read phase.
     *
     * @return true if the user clicked OK (proceed with read test),
     *         false if the user dismissed or cancelled
     */
    @Override
    public boolean confirmReadAfterWrite() {
        int result = JOptionPane.showConfirmDialog(
                Gui.mainFrame,
                """
                        For valid READ measurements please clear the disk cache by
                        using the included RAMMap.exe or flushmem.exe utilities.
                        Removable drives can be disconnected and reconnected.
                        For system drives use the WRITE and READ operations\s
                        independantly by doing a cold reboot after the WRITE""",
                "Clear Disk Cache Now",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        return result == JOptionPane.OK_OPTION;
    }

    /**
     * Refreshes the write/read metrics labels in the main frame.
     * App.updateMetrics() has already updated App.wAvg / rAvg etc. by the time
     * this is called, so refreshing the labels picks up the latest values.
     */
    @Override
    public void updateStats(double mbPerSec, double cumAvg, double cumMax) {
        SwingUtilities.invokeLater(() -> {
            if (App.writeTest) {
                Gui.mainFrame.refreshWriteMetrics();
            }
            if (App.readTest) {
                Gui.mainFrame.refreshReadMetrics();
            }
        });
    }

    /**
     * Delegates to the SwingWorker's built-in cancel signal so that
     * {@code swingRunner.cancel(true)} is the single point of cancellation —
     * no duplicate flag needed in DiskWorker.
     */
    @Override
    public boolean isCancelled() {
        return swingRunner != null && swingRunner.isCancelled();
    }
}
