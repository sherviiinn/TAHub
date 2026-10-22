package ir.TAHub.TAHub.util;

/** Converts Persian and Arabic digits to ASCII digits, so "۴۰۱۲" becomes "4012". */
public final class Digits {

    private Digits() {
    }

    public static String toAscii(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= '\u06F0' && c <= '\u06F9') {        // Persian digits
                result.append((char) ('0' + (c - '\u06F0')));
            } else if (c >= '\u0660' && c <= '\u0669') { // Arabic digits
                result.append((char) ('0' + (c - '\u0660')));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}