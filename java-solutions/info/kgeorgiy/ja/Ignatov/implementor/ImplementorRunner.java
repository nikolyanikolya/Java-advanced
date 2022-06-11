package info.kgeorgiy.ja.Ignatov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.nio.file.Paths;


/**
 * Class for running Implementor.main()
 *
 * @see Implementor
 */
public class ImplementorRunner {
    /**
     * This method takes program arguments and produces the implementing class for the input class or interface.
     * With option <var>-jar</var> it additionally makes the jar file for the <var>.class</var> files.
     *
     * @param args <var>Implementor.main()</var> program arguments. The method run expects two or three arguments:
     *             <ol>
     *             <li> option <var>-jar</var> (additionally)</li>
     *             <li> full class or interface name to generate implementation for</li>
     *             <li> Path to the project directory or jar-file name in case of option <var>-jar</var></li>
     *             </ol>
     * @throws ImplerException        when errors occurred while implementing the interface.
     *                                or class or making the jar file for it.
     * @throws ClassNotFoundException when input class or interface to generate implementation for is not found.
     */
    public static void run(String[] args) throws ImplerException, ClassNotFoundException {
        if (args == null || (args.length != 2 && args.length != 3)) {
            throw new IllegalArgumentException("Illegal number of arguments: two or three expected");
        }
        Implementor jarImplementor = new Implementor();

        if (args[0].equals("-jar")) {
            if (args.length != 3) {
                throw new IllegalArgumentException("Illegal number of arguments: three expected");
            }
            jarImplementor.implementJar(Class.forName(args[1]),
                    Paths.get(args[2]));
        } else {
            if (args.length != 2) {
                throw new IllegalArgumentException("Illegal number of arguments: two expected");
            }
            jarImplementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        }
    }
}
