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

        if (groupsFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(groupsFile), Charset.forName("UTF-8")))) {
                groupsConfig = configYaml.loadAs(reader, GroupsConfig.class);
                saveConfig();
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
            LOGGER.info("Permissions loaded!");
        } catch (CircularDependenciesException e) {
            LOGGER.error("Could not update permission groups because of circular dependencies!", e.getMessage());
        }
    }

    public void saveConfig() {
        String output = configYaml.dumpAsMap(groupsConfig);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(groupsFile), Charset.forName("UTF-8")))) {
            writer.write(output);
        } catch (Exception e) {
            LOGGER.error("Could not save config!", e);
        }
    }
}
