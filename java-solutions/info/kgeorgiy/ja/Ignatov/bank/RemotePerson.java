package info.kgeorgiy.ja.Ignatov.bank;

/**
 * Class represents individuals for remote operations with {@link Account}
 */
public class RemotePerson extends AbstractPerson {
    /**
     * RemotePerson constructor
     *
     * @param name     individual name
     * @param surname  individual surname
     * @param passport individual passport
     */
    public RemotePerson(String name, String surname, String passport) {
        super(name, surname, passport);
    }
}
