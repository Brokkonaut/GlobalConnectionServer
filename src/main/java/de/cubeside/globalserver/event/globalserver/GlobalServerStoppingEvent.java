package de.cubeside.globalserver.event.globalserver;

import de.cubeside.globalserver.GlobalServer;

public class GlobalServerStoppingEvent extends GlobalServerEvent {
    public GlobalServerStoppingEvent(GlobalServer globalServer) {
        super(globalServer);
    }
}
