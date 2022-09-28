package de.cubeside.globalserver.event.globalserver;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.event.Event;

public class GlobalServerEvent extends Event {
    private final GlobalServer globalServer;

    public GlobalServerEvent(GlobalServer globalServer) {
        this.globalServer = globalServer;
    }

    public GlobalServer getGlobalServer() {
        return globalServer;
    }
}
