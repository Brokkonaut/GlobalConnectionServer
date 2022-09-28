package de.cubeside.globalserver.event.clientconnection;

import de.cubeside.globalserver.ClientConnection;

public class ClientConnectionDissolveEvent extends ClientConnectionEvent {
    public ClientConnectionDissolveEvent(ClientConnection clientConnection) {
        super(clientConnection);
    }
}
