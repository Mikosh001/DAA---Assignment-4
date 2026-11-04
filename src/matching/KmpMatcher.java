package matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KmpMatcher {
    private KmpMatcher() { }

    public static MatchingResult findAll(String text, String pattern) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(pattern);
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return MatchingResult.emptyPattern(n);
        if (m > n) return new MatchingResult(List.of(), 0, 0, 0);
        int[] lps = new int[m];
        long preprocessingComparisons = 0;
        for (int i = 1, length = 0; i < m;) {
            preprocessingComparisons++;
            if (pattern.charAt(i) == pattern.charAt(length)) {
                lps[i++] = ++length;
            } else if (length > 0) {
                length = lps[length - 1];
            } else {
                lps[i++] = 0;
            }
        }
        List<Integer> indices = new ArrayList<>();
        long searchComparisons = 0;
        int i = 0;
        int j = 0;
        while (i < n) {
            searchComparisons++;
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == m) {
                    indices.add(i - m);
                    j = lps[j - 1];
                }
            } else if (j > 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return new MatchingResult(indices, preprocessingComparisons, searchComparisons, 0);
    }
}
