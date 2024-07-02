package de.cubeside.globalserver.permissions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class PermissionGroup {
    // private final PermissionSystem permissionSystem;
    private final String name;
    private int priority;

    private final HashMap<String, Boolean> directPermissions = new HashMap<>();
    private HashSet<String> directDependencies = new HashSet<>();

    private HashMap<String, Boolean> editorDirectPermissions;
    private HashSet<String> editorDirectDependencies;
    private boolean haveToRecalculatePermissions;

    volatile HashMap<String, Boolean> resolvedPermissions = new HashMap<>();

    public PermissionGroup(PermissionSystem permissionSystem, String name) {
        // this.permissionSystem = permissionSystem;
        this.name = name;
    }

    void addEditorPermission(String permission, boolean value) {
        if (editorDirectPermissions == null) {
            editorDirectPermissions = new HashMap<>(directPermissions);
        }
        editorDirectPermissions.put(permission, value);
    }

    void removeEditorPermission(String permission) {
        if (editorDirectPermissions == null) {
            editorDirectPermissions = new HashMap<>(directPermissions);
        }
        editorDirectPermissions.remove(permission);
    }

    boolean updateDependencies(HashMap<String, PermissionGroup> editorGroups, boolean hasNoNewOrRemovedGroups, ArrayDeque<PermissionGroup> groupStack) throws CircularDependenciesException {
        if (groupStack.contains(this)) {
            throw new CircularDependenciesException(this, groupStack);
        }
        // already checked
        if (editorDirectDependencies != null) {
            return haveToRecalculatePermissions;
        }
        // no changes
        if (editorDirectPermissions == null && hasNoNewOrRemovedGroups) {
            editorDirectDependencies = directDependencies;
            groupStack.addLast(this);
            for (String dependencyName : editorDirectDependencies) {
                PermissionGroup dependency = editorGroups.get(dependencyName);
                if (dependency != null) {
                    haveToRecalculatePermissions |= dependency.updateDependencies(editorGroups, hasNoNewOrRemovedGroups, groupStack);
                }
            }
            if (groupStack.removeLast() != this) {
                throw new IllegalStateException("broken group stack");
            }
            return haveToRecalculatePermissions;
        }
        // possible changes, so recalculate all direct dependencies
        if (editorDirectPermissions != null) {
            haveToRecalculatePermissions = true;
        }
        groupStack.addLast(this);
        editorDirectDependencies = new HashSet<>();
        HashMap<String, Boolean> perms = editorDirectPermissions != null ? editorDirectPermissions : directPermissions;
        for (String permission : perms.keySet()) {
            PermissionGroup dependency = editorGroups.get(permission);
            if (dependency != null) {
                editorDirectDependencies.add(permission);
                haveToRecalculatePermissions |= dependency.updateDependencies(editorGroups, hasNoNewOrRemovedGroups, groupStack);
            }
        }
        if (groupStack.removeLast() != this) {
            throw new IllegalStateException("broken group stack");
        }
        return haveToRecalculatePermissions;
    }

    void commitPermissionUpdates(HashMap<String, PermissionGroup> editorGroups) {
        if (editorDirectPermissions != null) {
            directPermissions.clear();
            directPermissions.putAll(editorDirectPermissions);
            editorDirectPermissions = null;
        }
        if (editorDirectDependencies != null && editorDirectDependencies != directDependencies) {
            directDependencies = editorDirectDependencies;
        }
        editorDirectDependencies = null;
        if (haveToRecalculatePermissions) {
            ArrayList<PermissionGroup> groupsByPriority = new ArrayList<>();
            for (String dependencyName : directDependencies) {
                PermissionGroup dependency = editorGroups.get(dependencyName);
                if (dependency != null) {
                    dependency.commitPermissionUpdates(editorGroups);
                    groupsByPriority.add(dependency);
                }
            }
            groupsByPriority.sort(PermissionGroupComparator.INSTANCE);

            // System.out.println("recalc " + name);
            HashMap<String, Boolean> resultingRermissions = new HashMap<>();
            HashSet<String> setByThisPriority = new HashSet<>();
            for (int i = 0; i < groupsByPriority.size(); i++) {
                PermissionGroup dependency = groupsByPriority.get(i);
                boolean nextHasSamePriority = false;
                if (i < groupsByPriority.size() - 1) {
                    PermissionGroup next = groupsByPriority.get(i + 1);
                    if (next.priority == dependency.priority) {
                        nextHasSamePriority = true;
                    }
                }
                Boolean dependencyValue = directPermissions.get(dependency.name);
                if (dependencyValue == null) {
                    throw new IllegalStateException("dependencyValue may not be null here");
                }
                // System.out.println(" " + dependency.getName() + " -> " + dependency.priority + " (" + dependencyValue + ")");
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
        haveToRecalculatePermissions = false;
    }

    void cancelPermissionUpdates() {
        editorDirectPermissions = null;
        editorDirectDependencies = null;
        haveToRecalculatePermissions = false;
    }

    public boolean hasPermission(String permission) {
        return resolvedPermissions.get(permission) == Boolean.TRUE;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    void setPriority(int priority) {
        this.priority = priority;
    }
}
