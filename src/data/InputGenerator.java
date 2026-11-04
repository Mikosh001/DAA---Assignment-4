package data;

import java.util.Random;
import matching.NaiveMatcher;

public final class InputGenerator {
    public static final String ALPHABET = "abcd";
    public static final long SEED = 20261104L;
    public static final int PATTERN_LENGTH = 32;

    private InputGenerator() { }

    public record MatchingInput(String text, String present, String absent) { }

    public static String randomString(int length, Random random) {
        char[] characters = new char[length];
        for (int i = 0; i < length; i++) {
            characters[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return new String(characters);
    }

    public static MatchingInput matching(int textLength, long seed) {
        if (textLength < PATTERN_LENGTH) throw new IllegalArgumentException("Text is too short");
        Random random = new Random(seed);
        String text = randomString(textLength, random);
        int start = (textLength - PATTERN_LENGTH) / 2;
        String present = text.substring(start, start + PATTERN_LENGTH);
        String absent;
        do {
            absent = randomString(PATTERN_LENGTH, random);
        } while (!NaiveMatcher.findAll(text, absent).indices().isEmpty());
        return new MatchingInput(text, present, absent);
    }
}
