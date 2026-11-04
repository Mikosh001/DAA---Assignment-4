package suffix;

import java.util.Arrays;
import java.util.Objects;

public final class SuffixArrayInput {
    private SuffixArrayInput() { }

    public static int[] build(String text) {
        Objects.requireNonNull(text);
        Integer[] order = new Integer[text.length()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> compare(text, a, b));
        int[] suffixArray = new int[order.length];
        for (int i = 0; i < order.length; i++) suffixArray[i] = order[i];
        return suffixArray;
    }

    private static int compare(String text, int a, int b) {
        while (a < text.length() && b < text.length()) {
            int difference = Character.compare(text.charAt(a), text.charAt(b));
            if (difference != 0) return difference;
            a++;
            b++;
        }
        return Integer.compare(text.length() - a, text.length() - b);
    }
}
