package info.kgeorgiy.ja.Ignatov.textStatistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextStatistics {

    private static final String regex = "^([a-zA-Z]{2,8})(?:_([a-zA-Z]{2}|[0-9]{3})){0,2}" +
            "(?:_((?:[0-9][0-9a-zA-Z]{3}|[0-9a-zA-Z]{5,8})(?:(?:_|-)" +
            "(?:[0-9][0-9a-zA-Z]{3}|[0-9a-zA-Z]{5,8}))*)?)?(?:(?:_#|#)" +
            "(?:[a-zA-Z]{4}))?(?:-(?:key=\\\"[0-9a-zA-Z]\\\"\\/value=\\\"" +
            "[0-9a-zA-Z]{1,8}(?:-[0-9a-zA-Z]{1,8})*\\\"))?$";
    private static final Pattern pattern = Pattern.compile(regex);

    public static Locale getLocale(String text) {
        Matcher matcher = pattern.matcher(text);
        ArrayList<String> arguments = new ArrayList<>();
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (Objects.nonNull(matcher.group(i))) {
                    arguments.add(matcher.group(i));
                }
            }
        }
        if (arguments.isEmpty()) {
            System.err.println(text + " is invalid locale");
        }
        switch (arguments.size()) {
            case 1:
                return new Locale(arguments.get(0));
            case 2:
                return new Locale(arguments.get(0), arguments.get(1));
            case 3:
                return new Locale(arguments.get(0), arguments.get(1), arguments.get(2));
            default: {
                System.err.println("invalid arguments");
                return null;
            }
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 4 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.out.println("expected 4 arguments. Usage <input locale> <output local> <input file> <output file>");
            return;
        }
        Locale inputLocale = getLocale(args[0]);
        Locale outputLocale = getLocale(args[1]);

        assert inputLocale != null;
        assert outputLocale != null;

        ResourceBundle outputBundle = ResourceBundle.getBundle("info.kgeorgiy.ja.Ignatov.textStatistics.data",
                new Locale("ru"));
        List<String> allLines;
        try {
            allLines = Files.readAllLines(Paths.get(args[2]));
        } catch (IOException e) {
            System.err.printf("I/O errors occurred processing file %s", args[2]);
            return;
        }
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(args[2]))) {
            String text = String.join("\n", allLines);
            final BreakIterator wordIterator = BreakIterator.getWordInstance(inputLocale);
            final BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(inputLocale);
            final Collator collator = Collator.getInstance(inputLocale);
            final Data dataSentence = getDataByText(sentenceIterator, text, collator);
            final NumberFormat formatter = NumberFormat.getNumberInstance(inputLocale);
            final Data dataWord = getDataByText(wordIterator, text, collator);
            final NumberData dataNumber = getNumberByText(wordIterator, text, formatter);
            bufferedWriter.write(String.format("""
                            Анализируемый файл: %s
                            Сводная статистика
                            Статистика по предложениям
                            \tЧисло предложений: %s.
                            \t Минимальное предложение: %s.
                            \t Максимальное предложение: %s.
                            \tМинимальная длина предложения: %s.
                            \t Максимальная длина предложения: %s.
                            \tСредняя длина предложения: %s.
                            Статистика по словам
                            \tЧисло слов: %s.
                            \tМинимальное слово: %s.
                            \tМаксимальное слово: %s.
                            \tМинимальная длина слова: %s.
                            \tМаксимальная длина слова: %s.
                            \tСредняя длина слова: %s.
                            Статистика по числам
                            \tЧисло чисел: %s.
                            \tМинимальное число: %s.
                            \tМаксимальное число: %s.
                            \tСреднее число: %s.""",
                    outputBundle.getString(args[2]),
                    getIntegerOnLocale(inputLocale, dataSentence.cnt + 1),
                    outputBundle.getString(dataSentence.minSentence),
                    outputBundle.getString(dataSentence.maxSentence),
                    getIntegerOnLocale(inputLocale, dataSentence.minLength),
                    getIntegerOnLocale(inputLocale, dataSentence.maxLength),
                    getDoubleOnLocale(inputLocale, (double) dataSentence.sumLength / (dataSentence.cnt + 1)),

                    getIntegerOnLocale(inputLocale, dataWord.cnt + 1),
                    outputBundle.getString(dataWord.minSentence),
                    outputBundle.getString(dataWord.maxSentence),
                    getIntegerOnLocale(outputLocale, dataWord.minLength),
                    getIntegerOnLocale(outputLocale, dataWord.maxLength),
                    getDoubleOnLocale(outputLocale, (double) dataWord.sumLength / (dataSentence.cnt + 1)),

                    getIntegerOnLocale(outputLocale, dataNumber.cnt + 1),
                    getDoubleOnLocale(outputLocale, dataNumber.maxNumber),
                    getDoubleOnLocale(outputLocale, dataNumber.minNumber),
                    getDoubleOnLocale(outputLocale, dataNumber.sum / (dataNumber.cnt + 1))
                    ));
        } catch (IOException e) {
            System.err.printf("I/O errors occurred while processing %s for writing", args[3]);
        }
    }

    private static String getIntegerOnLocale(Locale inLocale, long number) {
        return getNumberOnLocale(NumberFormat::getIntegerInstance, inLocale, number);
    }

    private static String getDoubleOnLocale(Locale inLocale, double number) {
        return getNumberOnLocale(NumberFormat::getNumberInstance, inLocale, number);
    }

    private static String getNumberOnLocale
            (Function<Locale, NumberFormat> getFormat, Locale inLocale, Number number) {
        return getFormat.apply(inLocale).format(number);
    }

    private static class Data {
        int cnt;
        int maxLength;
        int minLength;
        String maxSentence;
        String minSentence;
        int sumLength;

        private Data(int cnt, int maxLength, int minLength, String maxSentence, String minSentence, int sumLength) {
            this.cnt = cnt;
            this.maxLength = maxLength;
            this.minLength = minLength;
            this.maxSentence = maxSentence;
            this.minSentence = minSentence;
            this.sumLength = sumLength;
        }

    }

    private static class NumberData {
        int cnt;
        double maxNumber;
        double minNumber;
        double sum;

        public NumberData(int cnt, double maxNumber, double minNumber, double sum) {
            this.cnt = cnt;
            this.maxNumber = maxNumber;
            this.minNumber = minNumber;
            this.sum = sum;
        }
    }

    private static Data getDataByText(BreakIterator breakIterator, String text, Collator collator) {
        int start = breakIterator.first();
        int cnt = 0;
        int maxLength = -1;
        int minLength = 1000000; // TODO
        int sumLength = 0;
        ArrayList<String> allSequences = new ArrayList<>();
        for (int end = breakIterator.next(); end != BreakIterator.DONE;
             start = end, end = breakIterator.next()) {

            String curSeq = text.substring(start, end);
            allSequences.add(curSeq);
            sumLength += curSeq.length();
            cnt++;
            if (curSeq.length() > maxLength) {
                maxLength = curSeq.length();
            }
            if (curSeq.length() < minLength) {
                minLength = curSeq.length();
            }
        }
        String maxSeq = allSequences.stream().max(collator).orElse("");
        String minSeq = allSequences.stream().min(collator).orElse("");
        return new Data(cnt, maxLength, minLength, maxSeq, minSeq, sumLength);
    }

    private static NumberData getNumberByText(BreakIterator breakIterator, String text, NumberFormat formatter) {
        int start = breakIterator.first();
        int cnt = 0;
        double maxNumber = -1;
        double minNumber = 100000; // TODO
        double sum = 0;
        for (int end = breakIterator.next(); end != BreakIterator.DONE;
             start = end, end = breakIterator.next()) {

            String curSeq = text.substring(start, end);
            if (!isNumeric(curSeq, formatter)) {
                continue;
            }
            double curNumber;
            try {
                curNumber = (double) formatter.parse(curSeq);
            } catch (ParseException e) {
                continue;
            }
            maxNumber = Math.max(maxNumber, curNumber);
            minNumber = Math.min(minNumber, curNumber);
            cnt++;
            sum += curNumber;
        }
        return new NumberData(cnt, maxNumber, minNumber, sum);
    }

    private static boolean isNumeric(String str, NumberFormat formatter) {
        ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
    }


}