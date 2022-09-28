package de.cubeside.globalserver.event.clientconnection;

import de.cubeside.globalserver.ClientConnection;
import de.cubeside.globalserver.event.Event;

public class ClientConnectionEvent extends Event {
    private final ClientConnection clientConnection;

    public ClientConnectionEvent(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    public ClientConnection getClientConnection() {
        return clientConnection;
    }
}
