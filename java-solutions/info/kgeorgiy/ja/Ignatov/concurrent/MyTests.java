package info.kgeorgiy.ja.Ignatov.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class MyTests {
    public static void main(String[] args) {
        ParallelMapperImpl mapper = new ParallelMapperImpl(10);
        List<Integer> list = new ArrayList<>(Collections.nCopies(20, 0));
        for (int i = 0; i < list.size(); i++) {
            list.set(i, i + 1);
        }
        Function<Integer, Integer> function = x -> 2 * x;

        try {
            final List<Integer> answer = mapper.map(function, list);
            for (var t : answer) {
                System.out.print(t + " ");
            }
            System.out.println();
        } catch (InterruptedException ignore) {

        }

    }

}
