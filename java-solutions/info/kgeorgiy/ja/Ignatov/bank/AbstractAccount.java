package info.kgeorgiy.ja.Ignatov.bank;

public abstract class AbstractAccount implements Account{
    private final String id;
    private int amount;

    /**
     * AbstractAccount constructor
     *
     * @param id account id
     */
    public AbstractAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        this.amount = amount;
    }
}
