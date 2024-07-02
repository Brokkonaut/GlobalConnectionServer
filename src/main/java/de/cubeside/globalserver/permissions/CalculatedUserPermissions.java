package de.cubeside.globalserver.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class CalculatedUserPermissions {
    private final HashMap<String, Boolean> directPermissions;
    private HashSet<String> directDependencies = new HashSet<>();
    private int useCounter;
    private HashMap<String, Boolean> resolvedPermissions;

    public CalculatedUserPermissions(HashMap<String, Boolean> directPermissions, ConcurrentHashMap<String, PermissionGroup> groups) {
        this.directPermissions = directPermissions;
        calculate(groups, false);
    }

    void calculate(Map<String, PermissionGroup> groups, boolean skipDirectDependencyUpdate) {
        if (!skipDirectDependencyUpdate) {
            recalculateDependencies(groups);
        }
        if (directPermissions.size() == 1 && directDependencies.size() == 1) {
            Entry<String, Boolean> perm = directPermissions.entrySet().iterator().next();
            if (perm.getValue() == Boolean.TRUE) {
                String dependencyName = perm.getKey();
                PermissionGroup dependency = groups.get(dependencyName);
                if (dependency != null) {
                    // we can use this permissions
                    resolvedPermissions = dependency.resolvedPermissions;
                    return;
                }
            }
        }
        ArrayList<PermissionGroup> groupsByPriority = new ArrayList<>();
        for (String dependencyName : directDependencies) {
            PermissionGroup dependency = groups.get(dependencyName);
            if (dependency != null) {
                groupsByPriority.add(dependency);
            }
        }
        groupsByPriority.sort(PermissionGroupComparator.INSTANCE);

        HashMap<String, Boolean> resultingRermissions = new HashMap<>();
        HashSet<String> setByThisPriority = new HashSet<>();
        for (int i = 0; i < groupsByPriority.size(); i++) {
            PermissionGroup dependency = groupsByPriority.get(i);
            boolean nextHasSamePriority = false;
            if (i < groupsByPriority.size() - 1) {
                PermissionGroup next = groupsByPriority.get(i + 1);
                if (next.getPriority() == dependency.getPriority()) {
                    nextHasSamePriority = true;
                }
            }
            Boolean dependencyValue = directPermissions.get(dependency.getName());
            if (dependencyValue == null) {
                throw new IllegalStateException("dependencyValue may not be null here");
            }
            // System.out.println(" " + dependency.getName() + " -> " + dependency.getPriority() + " (" + dependencyValue + ")");
            // add permissions from this dependency
            for (Entry<String, Boolean> e : dependency.resolvedPermissions.entrySet()) {
                String permission = e.getKey();
                Boolean permissionValue = e.getValue();
                // invert if the dependencys value is false
                if (dependencyValue == Boolean.FALSE) {
                    permissionValue = permissionValue == Boolean.TRUE ? Boolean.FALSE : Boolean.TRUE;
                }
                Boolean old = resultingRermissions.put(permission, permissionValue);
                if (old != null && old != permissionValue) {
                    // if the old value was set by a dependency with the same prio, and it has a different value we have to set it to false
                    if (setByThisPriority.contains(permission)) {
                        permissionValue = Boolean.FALSE;
                        resultingRermissions.put(permission, permissionValue);
                    }
                }
                // next has a the same priority so we have to check for collisions
                if (nextHasSamePriority) {
                    setByThisPriority.add(permission);
                }
            }
            // next has a new priority so we can overwrite everything
            if (!nextHasSamePriority) {
                setByThisPriority.clear();
            }
        }
        // add direct permissions (they always overwrite dependencies)
        for (Entry<String, Boolean> e : directPermissions.entrySet()) {
            String permission = e.getKey();
            Boolean permissionValue = e.getValue();
            resultingRermissions.put(permission, permissionValue);
        }
        resolvedPermissions = resultingRermissions;
    }

    private void recalculateDependencies(Map<String, PermissionGroup> groups) {
        directDependencies.clear();
        for (String permission : directPermissions.keySet()) {
            if (groups.containsKey(permission)) {
                directDependencies.add(permission);
            }
        }
    }

    void decreaseUseCounter() {
        useCounter--;
    }

    void increaseUseCounter() {
        useCounter++;
    }

    int getUseCounter() {
        return useCounter;
    }

    public boolean hasPermission(String permission) {
        return resolvedPermissions.get(permission) == Boolean.TRUE;
    }
}
