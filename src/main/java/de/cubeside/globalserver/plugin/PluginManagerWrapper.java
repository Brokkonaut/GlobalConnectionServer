package de.cubeside.globalserver.plugin;

import de.cubeside.globalserver.GlobalServer;

public class PluginManagerWrapper {

    private PluginManager pluginManager;

    public PluginManagerWrapper(GlobalServer server) {
        this.pluginManager = new PluginManager(server);
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public void loadPlugins() throws PluginLoadException {
        pluginManager.loadPlugins();
    }

    public void shutdown() {
        pluginManager.shutdown();
    }
}
