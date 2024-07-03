package de.cubeside.globalserver.permissions.impl;

import java.util.ArrayDeque;
import java.util.Iterator;

public class CircularDependenciesException extends Exception {
    private static final long serialVersionUID = 2251092425553701301L;

    public CircularDependenciesException(PermissionGroup thisGroup, ArrayDeque<PermissionGroup> groupStack) {
        super("Cicrular dependencies detected: " + createCircularGoupsString(thisGroup, groupStack));
    }

    private static String createCircularGoupsString(PermissionGroup thisGroup, ArrayDeque<PermissionGroup> groupStack) {
        StringBuilder sb = new StringBuilder();
        Iterator<PermissionGroup> it = groupStack.iterator();
        boolean found = false;
        while (it.hasNext()) {
            PermissionGroup group2 = it.next();
            if (group2 == thisGroup) {
                found = true;
            }
            if (found) {
                sb.append(group2.getName()).append(" - ");
            }
        }
        sb.append(thisGroup.getName());
        return sb.toString();
    }
}
