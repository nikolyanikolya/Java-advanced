package info.kgeorgiy.ja.Ignatov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Implementor implements Impler {
    private static final String NEW_LINE = System.lineSeparator();
    private static final String TAB = "\t";
    private static final String DOUBLE_TAB = TAB + TAB;
    private static final String DOUBLE_NEW_LINE = NEW_LINE + NEW_LINE;

    private record ExecutableWrapper(Executable executable, String name) {

        @Override
        public String toString() {
            return DOUBLE_TAB + "public " + ((executable instanceof Method)
                    ? ((Method) executable).getReturnType().getCanonicalName() + " " : "") +
                    name + "(" + getArguments(executable, true) + ")" +
                    (executable.getExceptionTypes().length == 0 ? " " : getExceptions(executable))
                    + "{" + NEW_LINE + DOUBLE_TAB + TAB + getImplementation() + NEW_LINE + DOUBLE_TAB +
                    "}" + DOUBLE_NEW_LINE;
        }

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
                modifierCheck(token, Modifier::isPrivate) ||
                token.isEnum()) {
            throw new ImplerException("Incorrect class or interface for implementation");
        }
        String packageName = token.getPackage().getName();
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
            bufferedWriter.write("}" + NEW_LINE);
        } catch (IOException e) {
            throw new ImplerException("Error while writing to the output file. " + e.getMessage());
        }

    }

    private boolean modifierCheck(Class<?> token, Function<Integer, Boolean> isSuitable) {
        return isSuitable.apply(token.getModifiers());
    }

    private String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

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

    private void implementClassSignature(Class<?> token, BufferedWriter bufferedWriter, final String packageName)
            throws IOException {
        final StringBuilder signature = new StringBuilder();
        if (!packageName.equals("")) {
            signature.append("package ").append(packageName).append(";").append(NEW_LINE);
        }
        signature.append(NEW_LINE).append("public class ").append(token.getSimpleName())
                .append("Impl ").append(token.isInterface() ? "implements " : "extends ").append(token.getCanonicalName()).append(" {").append(NEW_LINE);
        bufferedWriter.write(signature.toString());
    }

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

    private static String getArguments(Executable executable, boolean fullSignature) {
        return Arrays.stream(executable.getParameters()).
                map(par -> ((fullSignature) ? par.getType().getCanonicalName() + " " : "")
                        + par.getName())
                .collect(Collectors.joining(", "));
    }

    private static String getExceptions(Executable executable) {
        return Arrays.stream(executable.getExceptionTypes()).
                map(Class::getCanonicalName)
                .collect(Collectors.joining(", ", " throws ", " "));
    }

    private Predicate<Executable> getModifierPredicate(Function<Integer, Boolean> isSuitable) {
        return executable -> isSuitable.apply(executable.getModifiers());
    }

    private Predicate<Executable> getModifierPredicate(Function<Integer, Boolean> foo1, Function<Integer, Boolean> foo2) {
        return executable -> getModifierPredicate(foo1).test(executable)
                || getModifierPredicate(foo2).test(executable);
    }

    private List<Method> filterAbstractMethods(Method[] allMethods) {
        return Arrays.stream(allMethods)
                .filter(getModifierPredicate(Modifier::isAbstract))
                .filter(getModifierPredicate(Modifier::isPublic, Modifier::isProtected))
                .collect(Collectors.toList());

    }

    private void implementMethods(Class<?> token, BufferedWriter bufferedWriter) throws IOException {
        var abstractMethods = filterAbstractMethods(token.getMethods());
        for (var method : abstractMethods) {
            bufferedWriter.write(new ExecutableWrapper(method, method.getName()).toString());
        }
    }

    private void implementConstructors(Class<?> token, BufferedWriter bufferedWriter) throws IOException {
        for (var constructor : token.getDeclaredConstructors()) {
            bufferedWriter.write(new ExecutableWrapper(constructor, getClassName(token)).toString());
        }
    }

}
