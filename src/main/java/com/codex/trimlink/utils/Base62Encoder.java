package com.codex.trimlink.utils;

public class Base62Encoder {
    private static final String ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = ALLOWED_CHARACTERS.length();
    
    /**
     * Converts a base-10 long ID into a Base62 alphanumeric string
     */
    public static String encode(long value) {
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            long rem = value % BASE;
            sb.append(ALLOWED_CHARACTERS.charAt((int)rem));
            value = value / 62;
        }
        return sb.reverse().toString();
    }
}
