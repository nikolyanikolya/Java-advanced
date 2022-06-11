package info.kgeorgiy.ja.Ignatov.bank;

import java.io.Serial;
import java.io.Serializable;
/**
 * Serializable class-implementation of {@link Account}
 */
public class LocalAccount extends AbstractAccount implements Serializable {
    @Serial
    private static final long serialVersionUID = 3141592653589793238L;

    /**
     * LocalAccount constructor
     *
     * @param id account id
     */
    public LocalAccount(String id) {
        super(id);
    }
}
