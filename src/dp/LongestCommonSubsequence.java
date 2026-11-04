package dp;

import java.util.Objects;

public final class LongestCommonSubsequence {
    private LongestCommonSubsequence() { }

    public record Result(int length, long cells) { }

    public static Result solve(String first, String second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        int m = first.length();
        int n = second.length();
        if (m == 0 || n == 0) return new Result(0, 0);
        int[][] table = new int[m + 1][n + 1];
        long cells = 0;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (first.charAt(i - 1) == second.charAt(j - 1)) {
                    table[i][j] = table[i - 1][j - 1] + 1;
                } else {
                    table[i][j] = Math.max(table[i - 1][j], table[i][j - 1]);
                }
                cells++;
            }
        }
        return new Result(table[m][n], cells);
    }
}
