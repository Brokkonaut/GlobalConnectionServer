package de.cubeside.globalserver.event.clientconnection;

import de.cubeside.globalserver.ClientConnection;

public class ClientConnectionEstablishedEvent extends ClientConnectionEvent {
    public ClientConnectionEstablishedEvent(ClientConnection clientConnection) {
        super(clientConnection);
    }
}
