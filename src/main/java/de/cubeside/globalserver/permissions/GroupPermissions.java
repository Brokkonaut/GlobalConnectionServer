package de.cubeside.globalserver.permissions;

import java.util.HashMap;

public class GroupPermissions {
    private int priority;
    private HashMap<String, Boolean> permissions = new HashMap<>();

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public HashMap<String, Boolean> getPermissions() {
        return permissions;
    }

    public void setPermissions(HashMap<String, Boolean> permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "{priority=" + priority + ";permissions=" + permissions.toString() + "}";
    }
}
