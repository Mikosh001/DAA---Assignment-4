package matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RabinKarpMatcher {
    private static final long BASE = 256;
    private static final long MODULUS = 1_000_000_007L;

    private RabinKarpMatcher() { }

    public static MatchingResult findAll(String text, String pattern) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(pattern);
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return MatchingResult.emptyPattern(n);
        if (m > n) return new MatchingResult(List.of(), 0, 0, 0);
        long power = 1;
        for (int i = 1; i < m; i++) power = power * BASE % MODULUS;
        long patternHash = 0;
        long windowHash = 0;
        long hashUpdates = 0;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * BASE + pattern.charAt(i)) % MODULUS;
            windowHash = (windowHash * BASE + text.charAt(i)) % MODULUS;
            hashUpdates += 2;
        }
        List<Integer> indices = new ArrayList<>();
        long comparisons = 0;
        for (int start = 0; start <= n - m; start++) {
            if (patternHash == windowHash) {
                int j = 0;
                while (j < m) {
                    comparisons++;
                    if (text.charAt(start + j) != pattern.charAt(j)) break;
                    j++;
                }
                if (j == m) indices.add(start);
            }
            if (start < n - m) {
                long outgoing = text.charAt(start) * power % MODULUS;
                windowHash = (windowHash - outgoing + MODULUS) % MODULUS;
                windowHash = (windowHash * BASE + text.charAt(start + m)) % MODULUS;
                hashUpdates++;
            }
        }
        return new MatchingResult(indices, 0, comparisons, hashUpdates);
    }
}
