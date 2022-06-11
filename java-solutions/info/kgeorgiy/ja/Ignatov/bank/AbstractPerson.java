package info.kgeorgiy.ja.Ignatov.bank;

/**
 * Abstract class - implementation of {@link Person}
 */
public abstract class AbstractPerson implements Person {
    private final String name;
    private final String surname;
    private final String passport;

    /**
     * AbstractPerson constructor
     *
     * @param name     individual name
     * @param surname  individual surname
     * @param passport individual passport. All individual accounts are linked
     *                 to passport
     */
    public AbstractPerson(String name, String surname, String passport) {
        this.name = name;
        this.surname = surname;
        this.passport = passport;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurName() {
        return surname;
    }

    @Override
    public String getPassport() {
        return passport;
    }

}
