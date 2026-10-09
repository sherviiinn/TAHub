package ir.TAHub.TAHub.util;

import java.security.SecureRandom;

/** Generates short random join codes for course offerings. */
public final class JoinCodes {

    // No 0, O, 1, I or L, so codes are easy to read aloud and to type.
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;

    // SecureRandom is unpredictable, unlike java.util.Random.
    private static final SecureRandom RANDOM = new SecureRandom();

    private JoinCodes() {
    }

    public static String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}