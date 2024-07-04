package de.cubeside.globalserver.permissions;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerConfig;
import de.cubeside.globalserver.permissions.impl.CircularDependenciesException;
import de.cubeside.globalserver.permissions.impl.PermissionSystem;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class GlobalPermissions {
    public final static Logger LOGGER = LogManager.getLogger("Permissions");

    private final PermissionSystem permissionSystem;

    private final File groupsFile = new File("groups.yml");
    private final Yaml configYaml;
    private GroupsConfig groupsConfig;

    public GlobalPermissions(GlobalServer globalServer) {
        permissionSystem = new PermissionSystem();

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
        loaderOptions.setNestingDepthLimit(Integer.MAX_VALUE);
        Constructor constructor = new Constructor(ServerConfig.class, loaderOptions);
        TypeDescription groupsConfigDescription = new TypeDescription(GroupsConfig.class);
        groupsConfigDescription.addPropertyParameters("groups", String.class, GroupPermissions.class);
        constructor.addTypeDescription(groupsConfigDescription);
        TypeDescription goupPermissionsDescription = new TypeDescription(GroupPermissions.class);
        goupPermissionsDescription.addPropertyParameters("permissions", String.class, Boolean.class);
        constructor.addTypeDescription(goupPermissionsDescription);
        configYaml = new Yaml(constructor);

        reload();
    }

    public synchronized void reload() {
        if (groupsFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(groupsFile), Charset.forName("UTF-8")))) {
                groupsConfig = configYaml.loadAs(reader, GroupsConfig.class);
            } catch (Exception e) {
                LOGGER.error("Could not parse permission groups config!", e);
            }
        }
        if (groupsConfig == null) {
            groupsConfig = new GroupsConfig();

            LOGGER.info("Generating new permission groups config!");
            if (!groupsFile.exists()) {
                saveConfig();
            }
        }

        reloadAllGroups();
    }

    private void reloadAllGroups() {
        try {
            permissionSystem.editGroups(editor -> {
                editor.removeAllGroups();
                for (Entry<String, GroupPermissions> e : groupsConfig.getGroups().entrySet()) {
                    String groupName = e.getKey();
                    for (Entry<String, Boolean> p : e.getValue().getPermissions().entrySet()) {
                        editor.addPermissionToGroup(groupName, p.getKey(), p.getValue());
                    }
                }
            });
            LOGGER.info("Permissions reloaded!");
        } catch (CircularDependenciesException e) {
            LOGGER.error("Could not load permission groups because of circular dependencies: " + e.getMessage());
            try {
                permissionSystem.editGroups(editor -> {
                    editor.removeAllGroups();
                });
            } catch (CircularDependenciesException e1) {
                throw new RuntimeException("should be impossible");
            }
        }
    }

    public synchronized void saveConfig() {
        String output = configYaml.dumpAsMap(groupsConfig);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(groupsFile), Charset.forName("UTF-8")))) {
            writer.write(output);
        } catch (Exception e) {
            LOGGER.error("Could not save config!", e);
        }
    }

    public synchronized Collection<String> getAllGroups() {
        return new ArrayList<>(groupsConfig.getGroups().keySet());
    }

    public synchronized boolean hasGroup(String groupName) {
        return groupsConfig.getGroups().containsKey(groupName);
    }

    public synchronized boolean addGroup(String groupName) {
        if (hasGroup(groupName)) {
            return false;
        }
        try {
            permissionSystem.editGroups(editor -> {
                editor.createGroup(groupName);
            });
            groupsConfig.getGroups().put(groupName, new GroupPermissions());
            saveConfig();
        } catch (CircularDependenciesException e) {
            LOGGER.error("Could not update permission groups because of circular dependencies: " + e.getMessage());
            return false;
        }
        return true;
    }

    public synchronized int getGroupPriority(String groupName) {
        GroupPermissions perms = groupsConfig.getGroups().get(groupName);
        return perms == null ? 0 : perms.getPriority();
    }

    public synchronized Map<String, Boolean> getGroupPermissions(String groupName) {
        GroupPermissions perms = groupsConfig.getGroups().get(groupName);
        return perms == null ? Map.of() : Collections.unmodifiableMap(perms.getPermissions());
    }

    public synchronized boolean setGroupPriority(String groupName, int prio) {
        GroupPermissions perms = groupsConfig.getGroups().get(groupName);
        if (perms == null) {
            return false;
        }
        try {
            permissionSystem.editGroups(editor -> {
                editor.setGroupPriority(groupName, prio);
            });
            perms.setPriority(prio);
            saveConfig();
        } catch (CircularDependenciesException e) {
            LOGGER.error("Could not update permission groups because of circular dependencies: " + e.getMessage());
            return false;
        }
        return true;
    }

    public synchronized boolean addGroupPermission(String groupName, String permission, boolean value) {
        GroupPermissions perms = groupsConfig.getGroups().get(groupName);
        if (perms == null) {
            return false;
        }
        try {
            permissionSystem.editGroups(editor -> {
                editor.addPermissionToGroup(groupName, permission, value);
            });
            perms.getPermissions().put(permission, value);
            saveConfig();
        } catch (CircularDependenciesException e) {
            LOGGER.error("Could not update permission groups because of circular dependencies: " + e.getMessage());
            return false;
        }
        return true;
    }

    public synchronized boolean removeGroupPermission(String groupName, String permission) {
        GroupPermissions perms = groupsConfig.getGroups().get(groupName);
        if (perms == null) {
            return false;
        }
        try {
            permissionSystem.editGroups(editor -> {
                editor.removePermissionFromGroup(groupName, permission);
            });
            perms.getPermissions().remove(permission);
            saveConfig();
        } catch (CircularDependenciesException e) {
            LOGGER.error("Could not update permission groups because of circular dependencies: " + e.getMessage());
            return false;
        }
        return true;
    }

    public synchronized boolean removeGroup(String groupName) {
        GroupPermissions perms = groupsConfig.getGroups().get(groupName);
        if (perms == null) {
            return false;
        }
        try {
            permissionSystem.editGroups(editor -> {
                editor.removeGroup(groupName);
            });
            groupsConfig.getGroups().remove(groupName);
            saveConfig();
        } catch (CircularDependenciesException e) {
            LOGGER.error("Could not update permission groups because of circular dependencies: " + e.getMessage());
            return false;
        }
        return true;
    }
}
