package de.cubeside.globalserver.utils;

public class Preconditions {
    public static <T> T notNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
