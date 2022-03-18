package info.kgeorgiy.ja.Ignatov.walk;

/**
 * @author Ignatov Nikolay
 */
public class Walk {
    public static void main(String[] args) {
        try {
            Walker.run(args);
        } catch (WalkException e) {
            System.err.println(e.getMessage());
        }
    }
}
