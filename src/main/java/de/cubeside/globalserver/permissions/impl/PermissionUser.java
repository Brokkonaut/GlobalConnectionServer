package de.cubeside.globalserver.permissions.impl;

import java.util.HashMap;

public class PermissionUser {
    private String name;
    private HashMap<String, Boolean> directPermissions = new HashMap<>();
    private HashMap<String, Boolean> directEditorPermissions;
    private volatile CalculatedUserPermissions calculatedPermissions;

    public PermissionUser(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    void addEditorPermission(String permission, boolean value) {
        if (directEditorPermissions == null) {
            directEditorPermissions = new HashMap<>(directPermissions);
        }
        directEditorPermissions.put(permission, value);
    }

    void removeEditorPermission(String permission) {
        if (directEditorPermissions == null) {
            directEditorPermissions = new HashMap<>(directPermissions);
        }
        directEditorPermissions.remove(permission);
    }

    void removeEditorAllPermissions() {
        if (directEditorPermissions == null) {
            directEditorPermissions = new HashMap<>();
        } else {
            directEditorPermissions.clear();
        }
    }

    void commit() {
        if (directEditorPermissions != null) {
            directPermissions = directEditorPermissions;
            directEditorPermissions = null;
        }
    }

    HashMap<String, Boolean> getDirectPermissions() {
        return directPermissions;
    }

    void setCalculatedPermissions(CalculatedUserPermissions calculated) {
        if (calculated != null) {
            calculated.increaseUseCounter();
        }
        if (this.calculatedPermissions != null) {
            this.calculatedPermissions.decreaseUseCounter();
        }
        this.calculatedPermissions = calculated;
    }

    public boolean hasPermission(String permission) {
        CalculatedUserPermissions local = calculatedPermissions;
        return local != null && local.hasPermission(permission);
    }
}
