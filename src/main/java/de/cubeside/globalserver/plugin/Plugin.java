package de.cubeside.globalserver.plugin;

import de.cubeside.globalserver.GlobalServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Plugin {
    private GlobalServer server;
    protected Logger logger;

    void initialize(GlobalServer server) {
        this.server = server;
        this.logger = LogManager.getLogger("Plugin(TODO)");
    }

    abstract public void onLoad(GlobalServer server);

    abstract public void onEnable();

    abstract public void onServerStarted();

    abstract public void onDisable();

    abstract public String getName();

    public GlobalServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
}
