package de.cubeside.globalserver.event.globalserver;

import de.cubeside.globalserver.GlobalServer;

public class GlobalServerStartedEvent extends GlobalServerEvent {
    public GlobalServerStartedEvent(GlobalServer globalServer) {
        super(globalServer);
    }
}
