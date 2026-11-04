package matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NaiveMatcher {
    private NaiveMatcher() { }

    public static MatchingResult findAll(String text, String pattern) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(pattern);
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return MatchingResult.emptyPattern(n);
        List<Integer> indices = new ArrayList<>();
        long comparisons = 0;
        for (int start = 0; start <= n - m; start++) {
            int j = 0;
            while (j < m) {
                comparisons++;
                if (text.charAt(start + j) != pattern.charAt(j)) break;
                j++;
            }
            if (j == m) indices.add(start);
        }
        return new MatchingResult(indices, 0, comparisons, 0);
    }
}
