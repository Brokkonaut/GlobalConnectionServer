package de.cubeside.globalserver.plugin;

import de.cubeside.globalserver.GlobalServer;
import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Plugin {
    private final GlobalServer server;
    private final Logger logger;
    private final PluginDescription description;
    private final File dataFolder;

    public Plugin() {
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader instanceof PluginClassLoader pluginClassLoader) {
            logger = LogManager.getLogger(pluginClassLoader.getPlugin().getName());
            server = pluginClassLoader.getServer();
            description = pluginClassLoader.getPlugin();
            dataFolder = new File(server.getPluginFolder(), description.getName());
        } else {
            logger = LogManager.getLogger(getClass().getName());
            logger.warn("Plugin " + getClass().getName() + " was not loaded by a PluginClassLoader");
            server = null;
            description = null;
            dataFolder = new File("./plugindata");
        }
    }

    public abstract void onLoad();

    public void onUnload() {

    }

    public final GlobalServer getServer() {
        return server;
    }

    public final Logger getLogger() {
        return logger;
    }

    public final PluginDescription getDescription() {
        return description;
    }

    public final File getDataFolder() {
        return dataFolder;
    }
}
