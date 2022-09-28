package de.cubeside.globalserver.event;

public class Priority {
    public static final int VERY_EARLY = -20000;
    public static final int EARLY = -10000;
    public static final int NORMAL = 0;
    public static final int LATE = 10000;
    public static final int VERY_LATE = 20000;
    public static final int MONITOR = Integer.MAX_VALUE;

    private Priority() {
        throw new IllegalArgumentException();
    }
}
