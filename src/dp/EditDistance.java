package dp;

import java.util.Objects;

public final class EditDistance {
    private EditDistance() { }

    public record Result(int distance, long cells) { }

    public static Result solve(String first, String second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        int m = first.length();
        int n = second.length();
        if (m == 0 || n == 0) return new Result(Math.max(m, n), 0);
        int[][] table = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) table[i][0] = i;
        for (int j = 0; j <= n; j++) table[0][j] = j;
        long cells = 0;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;
                table[i][j] = Math.min(table[i - 1][j] + 1,
                        Math.min(table[i][j - 1] + 1, table[i - 1][j - 1] + cost));
                cells++;
            }
        }
        return new Result(table[m][n], cells);
    }
}
