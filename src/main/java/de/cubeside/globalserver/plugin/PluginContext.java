package de.cubeside.globalserver.plugin;

import de.cubeside.globalserver.GlobalServer;
import java.net.MalformedURLException;

public class PluginContext {
    private final GlobalServer server;
    private final PluginDescription description;
    private final PluginClassLoader classLoader;
    private Plugin mainClassInstance;

    public PluginContext(GlobalServer server, PluginDescription description) throws PluginContextException {
        this.server = server;
        this.description = description;
        try {
            this.classLoader = new PluginClassLoader(server, description, getClass().getClassLoader());
        } catch (MalformedURLException e) {
            throw new PluginContextException("Could not create classloader for plugin " + description.getName() + ": " + e.getMessage(), e);
        }
    }

    void createMainClassInstance() throws PluginContextException {
        try {
            Class<?> main = classLoader.loadClass(description.getMainClass());
            if (!Plugin.class.isAssignableFrom(main)) {
                throw new ReflectiveOperationException("The specified class does not implement Plugin");
            }
            mainClassInstance = (Plugin) main.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | IllegalArgumentException | NoClassDefFoundError e) {
            throw new PluginContextException("Could not instantiate the main class '" + description.getMainClass() + "' for the plugin " + description.getName(), e);
        }
    }

    public PluginDescription getDescription() {
        return description;
    }

    public PluginClassLoader getClassLoader() {
        return classLoader;
    }

    public Plugin getMainClassInstance() {
        return mainClassInstance;
    }

    public GlobalServer getServer() {
        return server;
    }
}
