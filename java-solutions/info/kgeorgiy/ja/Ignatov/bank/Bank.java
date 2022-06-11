package info.kgeorgiy.ja.Ignatov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

/**
 * Interface representing bank operations with {@link Account} and {@link Person}.
 * Objects implementing this interface are transmitted via remote links
 */
public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it is not already exists.
     *
     * @param id account id
     * @return created or existing account.
     * @throws RemoteException when errors occurred while executing interface methods
     */
    Account createAccount(String id) throws RemoteException;

    /**
     * gets all account ids of provided person
     *
     * @param person local or remote person (individual)
     * @return account ids or empty set if no such person at bank
     * @throws RemoteException when errors occurred while executing interface methods
     */
    Set<String> getAllIds(Person person) throws RemoteException;

    /**
     * Checks whether person at the bank or not
     *
     * @param person local or remote person (individual)
     * @return true if bank has such person passport, otherwise false
     * @throws RemoteException when errors occurred while executing interface methods
     */
    boolean isExists(Person person) throws RemoteException;

    /**
     * gets {@link Account} of the provided person with specified account id
     *
     * @param person local or remote person
     * @param id     account id
     * @return Account of localPerson or remotePerson
     * @throws RemoteException when errors occurred while executing interface methods
     */

    Account getAccount(Person person, String id) throws RemoteException;

    /**
     * adds person with provided passport to the bank
     *
     * @param name     individual name
     * @param surname  individual surname
     * @param passport individual passport which serves for creating new accounts
     * @return true if person with such passport have not met before, false if passport is null
     * or such passport already exists
     * @throws RemoteException when errors occurred while executing interface methods
     */
    boolean createIndividual(String name, String surname, String passport) throws RemoteException;

    /**
     * gets {@link Person} at the bank by a provided passport
     *
     * @param passport individual passport
     * @param isLocal  flag which defines whether return
     *                 {@link LocalPerson}(true) or {@link RemotePerson} (false)
     * @return Person with provided passport at the bank or null if there are no
     * such passport at the bank or passport is null
     * @throws RemoteException when errors occurred while executing interface methods
     */
    Person getIndividual(String passport, boolean isLocal) throws RemoteException;


}
