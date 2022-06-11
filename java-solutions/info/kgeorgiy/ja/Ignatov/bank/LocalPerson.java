package info.kgeorgiy.ja.Ignatov.bank;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Serializable class represents individuals for local operations with {@link Account}
 */
public class LocalPerson extends AbstractPerson implements Serializable {
    @Serial
    private static final long serialVersionUID = 2718281828459045235L;
    private final ConcurrentMap<String, LocalAccount> accounts;

    /**
     * LocalPerson constructor
     *
     * @param name     individual name
     * @param surname  individual surname
     * @param passport individual passport. All individual accounts are linked
     *                 to passport
     * @param accounts mapping from an account id to an account
     * @see ConcurrentMap
     * @see Account
     */
    public LocalPerson(String name, String surname, String passport,
                       ConcurrentMap<String, LocalAccount> accounts) {
        super(name, surname, passport);
        this.accounts = accounts;
    }

    /**
     * allows to get an account by an id
     *
     * @param id account id
     * @return account by provided id
     */
    public Account getAccount(String id) {
        return accounts.get(id);
    }

    /**
     * allows to get all account ids of this localPerson
     *
     * @return account ids
     */
    public Set<String> getAllIds() {
        return accounts.keySet();
    }
}
