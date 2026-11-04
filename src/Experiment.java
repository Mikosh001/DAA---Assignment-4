import data.InputGenerator;
import dp.EditDistance;
import dp.LongestCommonSubsequence;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import matching.KmpMatcher;
import matching.MatchingResult;
import matching.NaiveMatcher;
import matching.RabinKarpMatcher;
import suffix.Kasai;
import suffix.SuffixArrayInput;

public final class Experiment {
    private static final int RUNS = 5;
    private static final int WARM_UPS = 5;

    private Experiment() { }

    public record Counts(long dpCells, long preprocessingComparisons,
                         long searchComparisons, long hashUpdates) {
        public String csv() {
            return dpCells + "," + preprocessingComparisons + "," + searchComparisons + "," + hashUpdates;
        }
    }

    public record Row(String algorithm, String inputCase, int n, int m, long seed,
                      double averageNs, Counts counts, String result) { }

    public static void run() throws IOException {
        Path tables = Path.of("results", "tables");
        Files.createDirectories(tables);
        List<Row> rows = new ArrayList<>();
        try (PrintWriter raw = new PrintWriter(Files.newBufferedWriter(tables.resolve("runs.csv")))) {
            raw.println("algorithm,input_case,n,m,seed,run,time_ns,dp_cells,preprocessing_comparisons,search_comparisons,hash_updates,result");
            for (int n : new int[]{100, 500, 1_000}) {
                long seed = InputGenerator.SEED + n;
                Random random = new Random(seed);
                String first = InputGenerator.randomString(n, random);
                String second = InputGenerator.randomString(n, random);
                int expectedLcs = LongestCommonSubsequence.solve(first, second).length();
                int expectedEdit = EditDistance.solve(first, second).distance();
                rows.add(measure(raw, "LCS", "random-pair", n, n, seed,
                        () -> LongestCommonSubsequence.solve(first, second),
                        result -> new Counts(result.cells(), 0, 0, 0),
                        result -> "length=" + result.length(),
                        result -> check(result.length() == expectedLcs, "LCS changed")));
                rows.add(measure(raw, "Edit Distance", "random-pair", n, n, seed,
                        () -> EditDistance.solve(first, second),
                        result -> new Counts(result.cells(), 0, 0, 0),
                        result -> "distance=" + result.distance(),
                        result -> check(result.distance() == expectedEdit, "Edit Distance changed")));
            }
            for (int n : new int[]{10_000, 50_000, 100_000}) {
                long seed = InputGenerator.SEED + 100_000 + n;
                InputGenerator.MatchingInput input = InputGenerator.matching(n, seed);
                for (String inputCase : new String[]{"present", "absent"}) {
                    String pattern = inputCase.equals("present") ? input.present() : input.absent();
                    List<Integer> expected = NaiveMatcher.findAll(input.text(), pattern).indices();
                    check(inputCase.equals("present") != expected.isEmpty(), "Incorrect pattern workload");
                    Consumer<MatchingResult> verify = result -> check(result.indices().equals(expected),
                            "String matchers disagree");
                    rows.add(measure(raw, "Naive", inputCase, n, pattern.length(), seed,
                            () -> NaiveMatcher.findAll(input.text(), pattern), Experiment::matchingCounts,
                            result -> "matches=" + result.indices().size(), verify));
                    rows.add(measure(raw, "KMP", inputCase, n, pattern.length(), seed,
                            () -> KmpMatcher.findAll(input.text(), pattern), Experiment::matchingCounts,
                            result -> "matches=" + result.indices().size(), verify));
                    rows.add(measure(raw, "Rabin-Karp", inputCase, n, pattern.length(), seed,
                            () -> RabinKarpMatcher.findAll(input.text(), pattern), Experiment::matchingCounts,
                            result -> "matches=" + result.indices().size(), verify));
                }
            }
            for (int n : new int[]{1_000, 5_000, 10_000}) {
                long seed = InputGenerator.SEED + 200_000 + n;
                String text = InputGenerator.randomString(n, new Random(seed));
                int[] suffixArray = SuffixArrayInput.build(text);
                int[] expected = referenceLcp(text, suffixArray);
                rows.add(measure(raw, "Kasai", "random-string", n, 0, seed,
                        () -> Kasai.build(text, suffixArray),
                        result -> new Counts(0, 0, result.comparisons(), 0),
                        result -> "max_lcp=" + Arrays.stream(result.lcp()).max().orElse(0),
                        result -> check(Arrays.equals(result.lcp(), expected), "Incorrect LCP array")));
            }
        }
        try (PrintWriter summary = new PrintWriter(Files.newBufferedWriter(tables.resolve("summary.csv")))) {
            String header = "algorithm,input_case,n,m,seed,runs,average_ns,dp_cells,preprocessing_comparisons,search_comparisons,hash_updates,result";
            summary.println(header);
            System.out.println(header);
            for (Row row : rows) {
                String line = String.format(Locale.US, "%s,%s,%d,%d,%d,%d,%.1f,%s,%s",
                        row.algorithm(), row.inputCase(), row.n(), row.m(), row.seed(), RUNS,
                        row.averageNs(), row.counts().csv(), row.result());
                summary.println(line);
                System.out.println(line);
            }
        }
        PlotWriter.write(rows);
        System.out.println("Saved " + rows.size() * RUNS + " measured runs, " + rows.size()
                + " summary rows and two plots in results/.");
    }

    private static <T> Row measure(PrintWriter raw, String algorithm, String inputCase,
                                   int n, int m, long seed, Supplier<T> operation,
                                   Function<T, Counts> getCounts, Function<T, String> describe,
                                   Consumer<T> verify) {
        for (int run = 0; run < WARM_UPS; run++) verify.accept(operation.get());
        long totalNs = 0;
        Counts firstCounts = null;
        String firstResult = null;
        for (int run = 1; run <= RUNS; run++) {
            long start = System.nanoTime();
            T result = operation.get();
            long elapsed = System.nanoTime() - start;
            totalNs += elapsed;
            verify.accept(result);
            Counts counts = getCounts.apply(result);
            String description = describe.apply(result);
            if (run == 1) {
                firstCounts = counts;
                firstResult = description;
            } else {
                check(firstCounts.equals(counts) && firstResult.equals(description),
                        "Repeated run changed results or counters");
            }
            raw.printf(Locale.US, "%s,%s,%d,%d,%d,%d,%d,%s,%s%n", algorithm, inputCase,
                    n, m, seed, run, elapsed, counts.csv(), description);
        }
        return new Row(algorithm, inputCase, n, m, seed, totalNs / (double) RUNS, firstCounts, firstResult);
    }

    private static Counts matchingCounts(MatchingResult result) {
        return new Counts(0, result.preprocessingComparisons(), result.searchComparisons(), result.hashUpdates());
    }

    private static int[] referenceLcp(String text, int[] suffixArray) {
        int[] lcp = new int[text.length()];
        for (int i = 1; i < suffixArray.length; i++) {
            int a = suffixArray[i - 1];
            int b = suffixArray[i];
            while (a + lcp[i] < text.length() && b + lcp[i] < text.length()
                    && text.charAt(a + lcp[i]) == text.charAt(b + lcp[i])) {
                lcp[i]++;
            }
        }
        return lcp;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
