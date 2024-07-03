package de.cubeside.globalserver.permissions;

import java.util.HashMap;

public class GroupsConfig {
    private HashMap<String, GroupPermissions> groups = new HashMap<>();

    public HashMap<String, GroupPermissions> getGroups() {
        return groups;
    }

    public void setGroups(HashMap<String, GroupPermissions> groups) {
        this.groups = groups;
    }

    @Override
    public String toString() {
        return groups.toString();
    }
}
