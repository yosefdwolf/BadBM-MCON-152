package edu.touro.mco152.bm.persist;

import edu.touro.mco152.bm.commands.BenchmarkObserver;
import jakarta.persistence.EntityManager;

/**
 * Observer that persists a completed {@link DiskRun} to the database.
 *
 * <p>Registered on {@link edu.touro.mco152.bm.commands.SimpleExecutor} so that
 * DB persistence is triggered automatically after any benchmark command finishes,
 * without the command classes needing to know about the EntityManager.
 */
public class DatabaseObserver implements BenchmarkObserver {

    /**
     * Persists the completed run to the database inside its own transaction.
     *
     * @param run the completed benchmark run to save
     */
    @Override
    public void onBenchmarkComplete(DiskRun run) {
        EntityManager em = EM.getEntityManager();
        em.getTransaction().begin();
        em.persist(run);
        em.getTransaction().commit();
    }
}
