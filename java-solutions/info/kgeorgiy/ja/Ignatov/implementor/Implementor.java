package info.kgeorgiy.ja.Ignatov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Class for the common functionality of {@link Impler} and {@link JarImpler}.
 * The class Implementor includes methods for generating of the interfaces implementation
 * and making the jar files.
 *
 * @author Ignatov Nikolay
 * @see ImplementorRunner
 * @see Impler
 * @see JarImpler
 */

public class Implementor implements Impler, JarImpler {

    /**
     * A constant for line separation. On UNIX systems, the value is "\n";
     * on Microsoft Windows systems it is "\r\n"
     */

    private static final String NEW_LINE = System.lineSeparator();

    /**
     * A constant for tabulation with value "\t"
     */

    private static final String TAB = "\t";

    /**
     * A constant for double tabulation with value "\t\t"
     */

    private static final String DOUBLE_TAB = TAB + TAB;

    /**
     * A constant for double newline. On UNIX systems, the value is "\n\n";
     * on Microsoft Windows systems it is "\r\n\r\n"
     */

    private static final String DOUBLE_NEW_LINE = NEW_LINE + NEW_LINE;

    /**
     * The record ExecutableWrapper allows serializing a wrapped executable object by {@link #toString()} method
     *
     * @see Executable
     */

    private record ExecutableWrapper(Executable executable, String name) {

        /**
         * Allows getting a string representation of the wrapped executable.
         * Using special methods in <var>{@link Class}</var>, <var>{@link Implementor}</var>
         * and <var>{@link #getImplementation()}</var>
         * it generates signature and implementation of provided executable.
         *
         * @return A string representation of the wrapped executable (constructor or method)
         * @see Executable
         */

        @Override
        public String toString() {
            return DOUBLE_TAB + "public " + ((executable instanceof Method)
                    ? ((Method) executable).getReturnType().getCanonicalName() + " " : "") +
                    name + "(" + getArguments(executable, true) + ")" +
                    (executable.getExceptionTypes().length == 0 ? " " : getExceptions(executable))
                    + "{" + NEW_LINE + DOUBLE_TAB + TAB + getImplementation() + NEW_LINE + DOUBLE_TAB +
                    "}" + DOUBLE_NEW_LINE;
        }

        /**
         * Serializes executable implementation.
         *
         * @return <ul>
         * <li>in the case of methods, the return value is a string representation of
         * the implementation of the method, exactly the return of the default value
         * </li>
         * <li>
         * in the case of constructors, the return value is a string representation of
         * the implementation of the constructor, exactly calling the superclass constructor with same arguments
         * </li>
         * <li>
         * otherwise ""
         * </li>
         * </ul>
         * @see Implementor#getDefaultValue(Class)
         * @see Implementor#getArguments(Executable, boolean)
         * @see Executable
         */

        private String getImplementation() {
            if (executable instanceof Method) {
                return "return " + getDefaultValue(((Method) executable).getReturnType()) + ";";
            }
            if (executable instanceof Constructor<?>) {
                return "super (" + getArguments(executable, false) + ");";
            }
            return "";
        }
    }

    @Override
    public void implement(Class<?> token, final Path root) throws ImplerException {
        if (token.isPrimitive() || token.isArray() ||
                modifierCheck(token, Modifier::isFinal) ||
                modifierCheck(token, Modifier::isPrivate)) {
            throw new ImplerException("Incorrect class or interface for implementation");
        }
        String packageName = getPackage(token);
        File output;
        try {
            output = getOutputFile(root, packageName, getClassName(token));
        } catch (IOException e) {
            throw new ImplerException("Error while creating the directories for the output file. " + e.getMessage());
        }
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(output))) {
            implementClassSignature(token, bufferedWriter, packageName);
            if (!token.isInterface()) {
                implementConstructors(token, bufferedWriter);
            }
            implementMethods(token, bufferedWriter);
            bufferedWriter.write(toUsAscii("}" + NEW_LINE));
        } catch (IOException e) {
            throw new ImplerException("Error while writing to the output file. " + e.getMessage());
        }

    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path root = Paths.get("").toAbsolutePath();
        implement(token, root);
        String className = Paths.get(getPackage(token).replace(".", File.separator))
                .resolve(getClassName(token)).toString();
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        var classFile = root.resolve(className) + ".java";
        if (!Files.exists(Path.of(classFile))) {
            throw new ImplerException("Class or interface not found");
        }
        String paths = "";
        try {
            var codeSource = token.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                var location = codeSource.getLocation();
                if (location != null) {
                    paths = root.getFileName() + File.pathSeparator + Path.of(location.toURI());
                }
            }
        } catch (URISyntaxException e) {
            throw new ImplerException("Error occurred while getting paths for the class or interface. "
                    + e.getMessage());
        }
        String[] javacOptions = {"-cp", paths, classFile, "-encoding", "us-Ascii"};
        int ret = javac.run(null, null, null, javacOptions);
        if (ret != 0) {
            throw new ImplerException("Errors occurred while compiling the class");
        }
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFile.toString()), manifest)) {
            target.putNextEntry(new JarEntry(className.replace(File.separatorChar, '/') + ".class"));
            Files.copy(Path.of(root.resolve(className) + ".class"), target);
        } catch (FileNotFoundException e) {
            throw new ImplerException("Jar file not found");
        } catch (IOException e) {
            throw new ImplerException("Errors occurred while adding files to the jar");
        }
    }

    /**
     * Checks the condition for <var>{@link Class}</var> token modifiers.
     *
     * @param token      type token to check modifiers for.
     * @param isSuitable boolean function for checking modifiers.
     * @return boolean value which is returned by isSuitable function for token`s modifiers.
     * @see Class
     * @see Function
     */

    private boolean modifierCheck(Class<?> token, Function<Integer, Boolean> isSuitable) {
        return isSuitable.apply(token.getModifiers());
    }

    /**
     * Allows getting a simple name of the implementing class.
     *
     * @param token type token to get the implementing class name for.
     * @return A string representation of the implementing class name.
     * @see Class
     */

    protected String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Allows getting a return value of the <var>{@link Class}</var> token.
     *
     * @param token type token to get default return value for.
     * @return Default return value for a provided token.
     * @see Class
     */

    private static String getDefaultValue(Class<?> token) {
        if (!token.isPrimitive()) {
            return null;
        }
        if (token == void.class) {
            return "";
        } else if (token == boolean.class) {
            return "false";
        } else {
            return "0";
        }
    }

    /**
     * Writes the package name and signature of the implementing class to the implementing class file.
     *
     * @param token          type token of class or interface to write implementing class signature for
     * @param bufferedWriter writer to the implementing class file
     * @param packageName    name of the implementing class package
     * @throws IOException when errors occurred while writing to the file
     * @see Class
     * @see BufferedWriter
     */

    private void implementClassSignature(Class<?> token, BufferedWriter bufferedWriter, final String packageName)
            throws IOException {
        final StringBuilder signature = new StringBuilder();
        if (!packageName.equals("")) {
            signature.append("package ").append(packageName).append(";").append(NEW_LINE);
        }
        signature.append(NEW_LINE).append("public class ").append(token.getSimpleName())
                .append("Impl ").append(token.isInterface() ? "implements " : "extends ")
                .append(token.getCanonicalName()).append(" {").append(NEW_LINE);
        bufferedWriter.write(toUsAscii(signature.toString()));
    }

    /**
     * Creates the implementing class file and all necessary directories for it using the provided path.
     *
     * @param root        path to the project directory.
     * @param packageName package name of the interface or class to generate implementation for.
     * @param simpleName  the class or interface name to generate implementation for.
     * @return Implementing class file.
     * @throws IOException when output file with specified path can not be created.
     * @see Path
     */

    private File getOutputFile(final Path root, final String packageName, final String simpleName) throws IOException {
        final Path filePath = Paths.get(Paths.get(root.toString(), packageName.split("\\."))
                .resolve(simpleName) + ".java");
        File output = new File(filePath.toString());
        if (!output.exists()) {
            final Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        }
        assert output.exists();
        return output;

    }

    /**
     * Serializes executable arguments.
     *
     * @param executable    executable to serialize its arguments for.
     * @param fullSignature flag controlling whether to output argument types or not.
     * @return A string representation of the executable arguments. If fullSignature is true,
     * argument types will be printed, otherwise - without the types.
     * @see Executable
     */

    private static String getArguments(Executable executable, boolean fullSignature) {
        return Arrays.stream(executable.getParameters()).
                map(par -> ((fullSignature) ? par.getType().getCanonicalName() + " " : "")
                        + par.getName())
                .collect(Collectors.joining(", "));
    }

    /**
     * Serializes executable exceptions.
     *
     * @param executable executable to serialize exceptions which it throws.
     * @return A string representation of exceptions which are thrown.
     * by provided executable or empty string if executable does not throw exceptions.
     * @see Executable
     */

    private static String getExceptions(Executable executable) {
        return Arrays.stream(executable.getExceptionTypes()).
                map(Class::getCanonicalName)
                .collect(Collectors.joining(", ", " throws ", " "));
    }

    /**
     * Allows getting the package name of the interface or class to generate implementation for.
     *
     * @param token type token to get the package of.
     * @return token package name or an empty string if the specified package is missing.
     * @see Class
     */

    protected String getPackage(Class<?> token) {
        var pack = token.getPackage();
        return pack == null ? "" : pack.getName();
    }

    /**
     * Allows getting <var>{@link Predicate}</var> for executable to check its modifiers.
     *
     * @param isSuitable function to check executable modifiers.
     * @return Predicate over boolean function.
     * @see Predicate
     * @see Executable
     * @see Function
     */

    private Predicate<Executable> getModifierPredicate(Function<Integer, Boolean> isSuitable) {
        return executable -> isSuitable.apply(executable.getModifiers());
    }

    /**
     * Filters abstract methods from an array of methods.
     *
     * @param allMethods {@link Method} array.
     * @return {@link List} of the abstract methods.
     * @see List
     * @see Method
     */

    private List<Method> filterAbstractMethods(Method[] allMethods) {
        return Arrays.stream(allMethods)
                .filter(getModifierPredicate(Modifier::isAbstract))
                .collect(Collectors.toList());

    }

    /**
     * Writes to the provided implementing class file an implementation of the class or interface methods.
     *
     * @param token          type token to implement its methods for.
     * @param bufferedWriter writer to the implementing class file.
     * @throws IOException when errors occurred while writing to the implementing class file.
     * @see Class
     * @see BufferedWriter
     */

    private void implementMethods(Class<?> token, BufferedWriter bufferedWriter) throws IOException {
        var abstractMethods = filterAbstractMethods(token.getMethods());
        for (var method : abstractMethods) {
            bufferedWriter.write(toUsAscii(new ExecutableWrapper(method, method.getName()).toString()));
        }
    }

    /**
     * Writes to the provided implementing class file an implementation of the class constructors.
     *
     * @param token          type token to implement its constructors for.
     * @param bufferedWriter writer to the implementing class file.
     * @throws IOException when errors occurred while writing to the implementing class file.
     * @see Class
     * @see BufferedWriter
     */

    private void implementConstructors(Class<?> token, BufferedWriter bufferedWriter) throws IOException {
        for (var constructor : token.getDeclaredConstructors()) {
            bufferedWriter.write(new ExecutableWrapper(constructor, getClassName(token)).toString());
        }
    }

    /**
     * Converts a string to us-Ascii encoding
     * @param source string to convert
     * @return converted to us-Ascii string
     */

    private static String toUsAscii(String source) {
        var stringBuilder = new StringBuilder();
        for (char c : source.toCharArray()) {
            stringBuilder.append(c < 128 ? c : String.format("\\u%04X", (int) c));
        }
        return stringBuilder.toString();
    }

    /**
     * This method takes program arguments and produces the implementing class for the input class or interface.
     * With option <var>-jar</var> it additionally makes the jar file for the <var>.class</var> files.
     *
     * @param args <var>program arguments</var>. The method expects two or three arguments:
     *             <ol>
     *                 <li> option <var>-jar</var> (additionally)</li>
     *                 <li> full class or interface name to generate implementation for</li>
     *                 <li> Path to the project directory or jar-file name in case of option <var>-jar</var></li>
     *             </ol>
     * @see ImplementorRunner#run(String[])
     */
    public static void main(String[] args) {
        try {
            ImplementorRunner.run(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Wrong arguments. " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Class is not found. " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Errors occurred while implementing the interface or class " +
                    "or making the jar file for it. "
                    + e.getMessage());
        }
    }

}
