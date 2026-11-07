package ir.TAHub.TAHub.util;

import java.security.SecureRandom;

/** Generates short random codes: join codes for course offerings, professor invite codes. */
public final class JoinCodes {

    // No 0, O, 1, I or L, so codes are easy to read aloud and to type.
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;

    // SecureRandom is unpredictable, unlike java.util.Random.
    private static final SecureRandom RANDOM = new SecureRandom();

    private JoinCodes() {
    }

    public static String generate() {
        return generate(LENGTH);
    }

    public static String generate(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
