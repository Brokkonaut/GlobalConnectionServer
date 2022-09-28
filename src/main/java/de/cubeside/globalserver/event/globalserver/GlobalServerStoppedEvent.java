package de.cubeside.globalserver.event.globalserver;

import de.cubeside.globalserver.GlobalServer;

public class GlobalServerStoppedEvent extends GlobalServerEvent {
    public GlobalServerStoppedEvent(GlobalServer globalServer) {
        super(globalServer);
    }
}
