package matching;

import java.util.ArrayList;
import java.util.List;

public record MatchingResult(List<Integer> indices, long preprocessingComparisons,
                             long searchComparisons, long hashUpdates) {
    public MatchingResult {
        indices = List.copyOf(indices);
    }

    static MatchingResult emptyPattern(int textLength) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i <= textLength; i++) indices.add(i);
        return new MatchingResult(indices, 0, 0, 0);
    }
}
