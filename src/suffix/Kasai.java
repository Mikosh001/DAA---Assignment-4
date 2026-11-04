package suffix;

import java.util.Arrays;
import java.util.Objects;

public final class Kasai {
    private Kasai() { }

    public record Result(int[] lcp, long comparisons) { }

    public static Result build(String text, int[] suffixArray) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(suffixArray);
        int n = text.length();
        if (suffixArray.length != n) throw new IllegalArgumentException("Suffix array length differs");
        int[] rank = new int[n];
        Arrays.fill(rank, -1);
        for (int i = 0; i < n; i++) {
            int suffix = suffixArray[i];
            if (suffix < 0 || suffix >= n || rank[suffix] != -1) {
                throw new IllegalArgumentException("Suffix array must be a permutation of 0..n-1");
            }
            rank[suffix] = i;
        }
        int[] lcp = new int[n];
        int common = 0;
        long comparisons = 0;
        for (int suffix = 0; suffix < n; suffix++) {
            int position = rank[suffix];
            if (position == 0) {
                common = 0;
                continue;
            }
            int previous = suffixArray[position - 1];
            while (suffix + common < n && previous + common < n) {
                comparisons++;
                if (text.charAt(suffix + common) != text.charAt(previous + common)) break;
                common++;
            }
            lcp[position] = common;
            if (common > 0) common--;
        }
        return new Result(lcp, comparisons);
    }
}
