/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.touro.mco152.bm.persist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import org.eclipse.persistence.exceptions.DatabaseException;

/**
 * Provides a static factory method for EntityManager objects.
 * This Entity Manager can get and set data from multiple
 * disk tests that were done.
 *
 * @author James
 */
public class EM {

    private static EntityManager em = null;

    /**
     * Gives you access to an EntityManager object that helps track and
     * manage changes done with persistent data from disk tests. This
     * method returns reference to the same object to all callers. A
     * new Entity Manager is only created when none already exists.
     * @return a reference to a EntityManger to help manage persistent data
     */
    public static EntityManager getEntityManager() {
        if (em == null) {
            try {
                EntityManagerFactory emf = Persistence.createEntityManagerFactory("jDiskMarkPU");
                em = emf.createEntityManager();
            } catch (PersistenceException jpe)
            {
                System.err.println("EM: FATAL Error initializing persistence: " + jpe.getMessage());
                if (jpe.getCause() instanceof DatabaseException)
                    System.err.println("Make sure another copy of program or DerbyDB is not already running");
                else
                    System.err.println("Make sure persistence.xml is accessible");

                System.exit(5);
            }
            catch (Exception exc)
            {
                System.err.println("EM: FATAL Error initializing persistence: " + exc.getMessage());
                System.err.println("Make sure persistence.xml is accessible");
                System.exit(5);
            }
        }
        return em;
    }
}
