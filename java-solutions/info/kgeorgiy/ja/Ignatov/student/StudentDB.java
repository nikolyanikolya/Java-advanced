package info.kgeorgiy.ja.Ignatov.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ignatov Nikolay
 */
public class StudentDB implements StudentQuery {
    @Override
    public List<String> getFirstNames(List<Student> students) {
        return applyMapToList(Student::getFirstName, students);

    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return applyMapToList(Student::getLastName, students);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return applyMapToList(Student::getGroup, students);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return applyMapToList(student -> student.getFirstName() + " " + student.getLastName(), students);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return applyMapToCollection(Student::getFirstName, students, TreeSet::new);
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Comparator.naturalOrder())
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortedToList(Comparator.naturalOrder(), students);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortedToList(comparator, students);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterByPredicateToList(CompareStrings(Student::getFirstName, name), students, comparator);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterByPredicateToList(CompareStrings(Student::getLastName, name), students, comparator);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return filterByPredicateToStream(CompareStrings(Student::getGroup, group), students, comparator)
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return filterByPredicateToStream(CompareStrings(Student::getGroup, group), students, comparator)
                .collect(Collectors.toMap(Student::getLastName,
                        Student::getFirstName,
                        // :NOTE: naturalOrder
                        BinaryOperator.minBy(String::compareTo)));
    }

    // :NOTE: static
    private final Comparator<Student> comparator = Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .reversed()
            .thenComparing(Comparator.naturalOrder());

    private <T, R extends Collection<T>> R applyMapToCollection(Function<Student, T> function,
                                                                List<Student> students, Supplier<R> supplier) {
        return students.stream()
                .map(function)
                .collect(Collectors.toCollection(supplier));
    }

    private <T> List<T> applyMapToList(Function<Student, T> function, List<Student> students) {
        return applyMapToCollection(function, students, ArrayList::new);
    }

    private List<Student> sortedToList(Comparator<Student> comparator,
                                       Collection<Student> students) {
        return students.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private Stream<Student> filterByPredicateToStream(Predicate<Student> predicate,
                                                      Collection<Student> students,
                                                      Comparator<Student> comparator) {
        return students.stream()
                .filter(predicate)
                .sorted(comparator);
    }

    private List<Student> filterByPredicateToList(Predicate<Student> predicate,
                                                  Collection<Student> students,
                                                  Comparator<Student> comparator) {
        return filterByPredicateToStream(predicate, students, comparator)
                // :NOTE: toList
                .collect(Collectors.toList());
    }

    // :NOTE: naming
    private <E> Predicate<Student> CompareStrings(Function<Student, E> function, E key) {
        return s -> Objects.equals(function.apply(s), key);
    }
}
