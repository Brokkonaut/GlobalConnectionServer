package de.cubeside.globalserver.permissions;

import java.util.Comparator;

public class PermissionGroupComparator implements Comparator<PermissionGroup> {
    public static PermissionGroupComparator INSTANCE = new PermissionGroupComparator();

    private PermissionGroupComparator() {
    }

    @Override
    public int compare(PermissionGroup g1, PermissionGroup g2) {
        int d = Integer.compare(g1.getPriority(), g2.getPriority());
        if (d != 0) {
            return d;
        }
        return g1.getName().compareTo(g2.getName());
    }
}
