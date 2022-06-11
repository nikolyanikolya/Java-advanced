package info.kgeorgiy.ja.Ignatov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface representing operations with individuals.
 * Objects implementing this interface are transmitted via remote links
 */
public interface Person extends Remote {
    /**
     * gets individual name
     *
     * @return individual name
     * @throws RemoteException when errors occurred while executing interface methods
     */
    String getName() throws RemoteException;

    /**
     * gets individual surname
     *
     * @return individual surname
     * @throws RemoteException when errors occurred while executing interface methods
     */
    String getSurName() throws RemoteException;

    /**
     * gets individual passport
     *
     * @return individual passport which is used for creating new accounts
     * @throws RemoteException when errors occurred while executing interface methods
     */
    String getPassport() throws RemoteException;
}
