package tests;

import data.InputGenerator;
import dp.EditDistance;
import dp.LongestCommonSubsequence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import matching.KmpMatcher;
import matching.MatchingResult;
import matching.NaiveMatcher;
import matching.RabinKarpMatcher;
import suffix.Kasai;
import suffix.SuffixArrayInput;

public final class AlgorithmTests {
    private static int passed;

    private AlgorithmTests() { }

    public static void main(String[] args) { run(); }

    public static void run() {
        passed = 0;
        test("LCS required cases and subsequence semantics", AlgorithmTests::lcsCases);
        test("LCS against exhaustive subsequence enumeration", AlgorithmTests::randomLcs);
        test("Edit Distance base cases and all three operations", AlgorithmTests::editCases);
        test("Edit Distance against a recursive reference", AlgorithmTests::randomEdit);
        test("Matches at beginning, middle and end", AlgorithmTests::matchingPositions);
        test("Multiple and overlapping occurrences", AlgorithmTests::matchingOverlaps);
        test("Absent, longer and empty patterns", AlgorithmTests::matchingEdges);
        test("All matchers against String.indexOf", AlgorithmTests::randomMatching);
        test("KMP fallback and operation counters", AlgorithmTests::matchingCounters);
        test("Rabin-Karp rejects an actual hash collision", AlgorithmTests::hashCollision);
        test("Kasai with manually verified banana arrays", AlgorithmTests::manualKasai);
        test("Kasai empty, single and repetitive strings", AlgorithmTests::kasaiEdges);
        test("Kasai against adjacent-suffix comparisons", AlgorithmTests::randomKasai);
        test("Kasai rejects invalid permutations", AlgorithmTests::invalidSuffixArrays);
        test("Required workloads, alphabet and fixed seeds", AlgorithmTests::workloads);
        System.out.println(passed + " tests passed.");
    }

    private static void test(String name, Runnable operation) {
        operation.run();
        passed++;
        System.out.println("PASS: " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void lcsCases() {
        check(LongestCommonSubsequence.solve("abc", "abc").length() == 3, "Identical LCS");
        check(LongestCommonSubsequence.solve("abc", "xyz").length() == 0, "Disjoint LCS");
        check(LongestCommonSubsequence.solve("", "abc").length() == 0, "Empty first LCS");
        check(LongestCommonSubsequence.solve("abc", "").length() == 0, "Empty second LCS");
        check(LongestCommonSubsequence.solve("", "").length() == 0, "Both empty LCS");
        check(LongestCommonSubsequence.solve("abc", "ac").length() == 2, "Not a substring problem");
        check(LongestCommonSubsequence.solve("ABCBDAB", "BDCABA").length() == 4, "Known LCS");
        check(LongestCommonSubsequence.solve("aaaa", "aa").length() == 2, "Repeated LCS");
        check(LongestCommonSubsequence.solve("abc", "ac").cells() == 6, "LCS cell counter");
    }

    private static void randomLcs() {
        Random random = new Random(41);
        for (int trial = 0; trial < 250; trial++) {
            String first = InputGenerator.randomString(random.nextInt(11), random);
            String second = InputGenerator.randomString(random.nextInt(11), random);
            int expected = exhaustiveLcs(first, second);
            check(LongestCommonSubsequence.solve(first, second).length() == expected, "Random LCS");
            check(LongestCommonSubsequence.solve(second, first).length() == expected, "LCS symmetry");
        }
    }

    private static int exhaustiveLcs(String first, String second) {
        int best = 0;
        for (int mask = 0; mask < (1 << first.length()); mask++) {
            int length = Integer.bitCount(mask);
            if (length <= best) continue;
            int position = 0;
            boolean found = true;
            for (int i = 0; i < first.length(); i++) {
                if ((mask & (1 << i)) == 0) continue;
                while (position < second.length() && second.charAt(position) != first.charAt(i)) position++;
                if (position == second.length()) {
                    found = false;
                    break;
                }
                position++;
            }
            if (found) best = length;
        }
        return best;
    }

    private static void editCases() {
        check(EditDistance.solve("abc", "abc").distance() == 0, "Identical edit");
        check(EditDistance.solve("", "abc").distance() == 3, "Insert all");
        check(EditDistance.solve("abc", "").distance() == 3, "Delete all");
        check(EditDistance.solve("", "").distance() == 0, "Both empty edit");
        check(EditDistance.solve("cat", "cut").distance() == 1, "Substitution");
        check(EditDistance.solve("cat", "cart").distance() == 1, "Insertion");
        check(EditDistance.solve("cart", "cat").distance() == 1, "Deletion");
        check(EditDistance.solve("kitten", "sitting").distance() == 3, "Known edit");
        check(EditDistance.solve("ab", "ba").distance() == 2, "Transposition is not one operation");
        check(EditDistance.solve("cat", "cut").cells() == 9, "Edit cell counter");
    }

    private static void randomEdit() {
        Random random = new Random(42);
        for (int trial = 0; trial < 150; trial++) {
            String first = InputGenerator.randomString(random.nextInt(6), random);
            String second = InputGenerator.randomString(random.nextInt(6), random);
            int expected = recursiveEdit(first, second, first.length(), second.length());
            check(EditDistance.solve(first, second).distance() == expected, "Random edit");
            check(EditDistance.solve(second, first).distance() == expected, "Edit symmetry");
        }
    }

    private static int recursiveEdit(String first, String second, int m, int n) {
        if (m == 0) return n;
        if (n == 0) return m;
        if (first.charAt(m - 1) == second.charAt(n - 1)) {
            return recursiveEdit(first, second, m - 1, n - 1);
        }
        return 1 + Math.min(recursiveEdit(first, second, m - 1, n),
                Math.min(recursiveEdit(first, second, m, n - 1),
                        recursiveEdit(first, second, m - 1, n - 1)));
    }

    private static void matchingPositions() {
        matches("abcXYZ", "abc", List.of(0));
        matches("zzabczz", "abc", List.of(2));
        matches("zzabc", "abc", List.of(2));
        matches("abc", "abc", List.of(0));
        matches("a", "a", List.of(0));
    }

    private static void matchingOverlaps() {
        matches("abcabcabc", "abc", List.of(0, 3, 6));
        matches("aaaaa", "aaa", List.of(0, 1, 2));
        matches("abababa", "aba", List.of(0, 2, 4));
        matches("aaaa", "a", List.of(0, 1, 2, 3));
        matches("x\uD83D\uDE00x\uD83D\uDE00", "\uD83D\uDE00", List.of(1, 4));
    }

    private static void matchingEdges() {
        matches("abc", "z", List.of());
        matches("ab", "abc", List.of());
        matches("", "a", List.of());
        matches("abc", "", List.of(0, 1, 2, 3));
        matches("", "", List.of(0));
    }

    private static void matches(String text, String pattern, List<Integer> expected) {
        check(NaiveMatcher.findAll(text, pattern).indices().equals(expected), "Naive indices");
        check(KmpMatcher.findAll(text, pattern).indices().equals(expected), "KMP indices");
        check(RabinKarpMatcher.findAll(text, pattern).indices().equals(expected), "Rabin-Karp indices");
    }

    private static List<Integer> referenceMatches(String text, String pattern) {
        List<Integer> indices = new ArrayList<>();
        for (int start = 0; start <= text.length();) {
            int match = text.indexOf(pattern, start);
            if (match < 0) break;
            indices.add(match);
            start = match + 1;
        }
        return indices;
    }

    private static void randomMatching() {
        Random random = new Random(43);
        for (int trial = 0; trial < 600; trial++) {
            String text = InputGenerator.randomString(random.nextInt(201), random);
            String pattern = InputGenerator.randomString(random.nextInt(41), random);
            if (!text.isEmpty() && trial % 3 == 0) {
                int start = random.nextInt(text.length());
                pattern = text.substring(start, Math.min(text.length(), start + random.nextInt(20)));
            }
            matches(text, pattern, referenceMatches(text, pattern));
        }
    }

    private static void matchingCounters() {
        String text = "a".repeat(10_000);
        String pattern = "a".repeat(64) + "b";
        matches(text, pattern, List.of());
        MatchingResult naive = NaiveMatcher.findAll(text, pattern);
        MatchingResult kmp = KmpMatcher.findAll(text, pattern);
        MatchingResult rabinKarp = RabinKarpMatcher.findAll(text, pattern);
        check(naive.searchComparisons() == (long) (text.length() - pattern.length() + 1) * pattern.length(),
                "Naive worst-case counter");
        check(kmp.preprocessingComparisons() <= 2L * pattern.length(), "LPS must be linear");
        check(kmp.searchComparisons() <= 2L * text.length(), "KMP search must be linear");
        check(rabinKarp.hashUpdates() == (long) text.length() + pattern.length(), "Rolling hash updates");
        MatchingResult overlapping = KmpMatcher.findAll("abababab", "abab");
        check(overlapping.indices().equals(List.of(0, 2, 4)), "LPS preserves overlaps");
        check(overlapping.preprocessingComparisons() == 3, "Exact LPS comparison count");
    }

    private static void hashCollision() {
        String text = new String(new char[]{1, 257});
        String pattern = new String(new char[]{2, 1});
        check(256 * text.charAt(0) + text.charAt(1)
                == 256 * pattern.charAt(0) + pattern.charAt(1), "Constructed hash collision");
        matches(text, pattern, List.of());
        check(RabinKarpMatcher.findAll(text, pattern).searchComparisons() == 1,
                "Collision must trigger character verification");
    }

    private static void manualKasai() {
        int[] suffixArray = {5, 3, 1, 0, 4, 2};
        int[] original = suffixArray.clone();
        check(Arrays.equals(SuffixArrayInput.build("banana"), suffixArray), "Manual suffix array");
        check(Arrays.equals(Kasai.build("banana", suffixArray).lcp(), new int[]{0, 1, 3, 0, 0, 2}),
                "Manual LCP array");
        check(Arrays.equals(suffixArray, original), "Suffix array input changed");
    }

    private static void kasaiEdges() {
        check(Kasai.build("", new int[0]).lcp().length == 0, "Empty LCP");
        check(Arrays.equals(Kasai.build("a", new int[]{0}).lcp(), new int[]{0}), "Single LCP");
        check(Arrays.equals(Kasai.build("abc", new int[]{0, 1, 2}).lcp(), new int[]{0, 0, 0}),
                "No common prefixes");
        int n = 2_000;
        int[] suffixArray = new int[n];
        int[] expected = new int[n];
        for (int i = 0; i < n; i++) {
            suffixArray[i] = n - 1 - i;
            expected[i] = i;
        }
        Kasai.Result result = Kasai.build("a".repeat(n), suffixArray);
        check(Arrays.equals(result.lcp(), expected), "Repetitive LCP");
        check(result.comparisons() <= 3L * n, "Kasai must reuse common prefixes");
    }

    private static void randomKasai() {
        Random random = new Random(44);
        for (int trial = 0; trial < 200; trial++) {
            String text = InputGenerator.randomString(random.nextInt(101), random);
            verifyKasai(text, SuffixArrayInput.build(text));
        }
    }

    private static void verifyKasai(String text, int[] suffixArray) {
        int[] expected = new int[text.length()];
        for (int i = 1; i < suffixArray.length; i++) {
            String previous = text.substring(suffixArray[i - 1]);
            String current = text.substring(suffixArray[i]);
            check(previous.compareTo(current) < 0, "Suffix array is not sorted");
            while (expected[i] < previous.length() && expected[i] < current.length()
                    && previous.charAt(expected[i]) == current.charAt(expected[i])) {
                expected[i]++;
            }
        }
        Kasai.Result actual = Kasai.build(text, suffixArray);
        check(Arrays.equals(actual.lcp(), expected), "Adjacent-suffix LCP reference");
        check(actual.comparisons() <= 3L * text.length(), "Kasai linear comparison bound");
    }

    private static void invalidSuffixArrays() {
        rejects(() -> Kasai.build("abc", new int[]{0, 1}));
        rejects(() -> Kasai.build("abc", new int[]{0, 0, 2}));
        rejects(() -> Kasai.build("abc", new int[]{-1, 1, 2}));
        rejects(() -> Kasai.build("abc", new int[]{0, 1, 3}));
    }

    private static void rejects(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void workloads() {
        for (int n : new int[]{100, 500, 1_000}) {
            Random firstRandom = new Random(InputGenerator.SEED + n);
            Random secondRandom = new Random(InputGenerator.SEED + n);
            for (int i = 0; i < 2; i++) {
                String first = InputGenerator.randomString(n, firstRandom);
                String second = InputGenerator.randomString(n, secondRandom);
                check(first.equals(second) && first.length() == n, "Reproducible DP input");
                checkAlphabet(first);
            }
        }
        for (int n : new int[]{10_000, 50_000, 100_000}) {
            long seed = InputGenerator.SEED + 100_000 + n;
            InputGenerator.MatchingInput first = InputGenerator.matching(n, seed);
            check(first.equals(InputGenerator.matching(n, seed)), "Reproducible matching input");
            check(first.text().length() == n, "Required text size");
            check(first.present().length() == 32 && first.absent().length() == 32, "Pattern length");
            checkAlphabet(first.text());
            checkAlphabet(first.present());
            checkAlphabet(first.absent());
            List<Integer> present = referenceMatches(first.text(), first.present());
            check(!present.isEmpty(), "Present workload has no match");
            matches(first.text(), first.present(), present);
            matches(first.text(), first.absent(), List.of());
        }
        for (int n : new int[]{1_000, 5_000, 10_000}) {
            long seed = InputGenerator.SEED + 200_000 + n;
            String text = InputGenerator.randomString(n, new Random(seed));
            check(text.equals(InputGenerator.randomString(n, new Random(seed))), "Reproducible Kasai input");
            checkAlphabet(text);
            verifyKasai(text, SuffixArrayInput.build(text));
        }
    }

    private static void checkAlphabet(String text) {
        for (int i = 0; i < text.length(); i++) {
            check(InputGenerator.ALPHABET.indexOf(text.charAt(i)) >= 0, "Inconsistent alphabet");
        }
    }
}
