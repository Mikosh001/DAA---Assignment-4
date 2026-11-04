import dp.EditDistance;
import dp.LongestCommonSubsequence;
import java.io.IOException;
import java.util.Arrays;
import matching.KmpMatcher;
import matching.NaiveMatcher;
import matching.RabinKarpMatcher;
import suffix.Kasai;
import tests.AlgorithmTests;

public final class Main {
    private Main() { }

    public static void main(String[] args) throws IOException {
        String mode = args.length == 0 ? "demo" : args[0];
        switch (mode) {
            case "demo" -> demo();
            case "test" -> AlgorithmTests.run();
            case "experiment" -> Experiment.run();
            default -> throw new IllegalArgumentException("Use demo, test, or experiment");
        }
    }

    private static void demo() {
        System.out.println("LCS(ABCBDAB, BDCABA): "
                + LongestCommonSubsequence.solve("ABCBDAB", "BDCABA").length());
        System.out.println("Edit Distance(kitten, sitting): "
                + EditDistance.solve("kitten", "sitting").distance());
        String text = "ababa";
        String pattern = "aba";
        System.out.println("Matching text=ababa, pattern=aba");
        System.out.println("Naive: " + NaiveMatcher.findAll(text, pattern).indices());
        System.out.println("KMP: " + KmpMatcher.findAll(text, pattern).indices());
        System.out.println("Rabin-Karp: " + RabinKarpMatcher.findAll(text, pattern).indices());
        int[] suffixArray = {5, 3, 1, 0, 4, 2};
        System.out.println("banana suffix array: " + Arrays.toString(suffixArray));
        System.out.println("banana LCP array: " + Arrays.toString(Kasai.build("banana", suffixArray).lcp()));
    }
}
