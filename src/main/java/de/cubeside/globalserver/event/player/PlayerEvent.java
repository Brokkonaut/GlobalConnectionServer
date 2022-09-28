package de.cubeside.globalserver.event.player;

import de.cubeside.globalserver.ClientConnection;
import de.cubeside.globalserver.OnlinePlayer;
import de.cubeside.globalserver.event.Event;

public class PlayerEvent extends Event {
    private final OnlinePlayer onlinePlayer;
    private final ClientConnection clientConnection;

    public PlayerEvent(ClientConnection clientConnection, OnlinePlayer onlinePlayer) {
        this.clientConnection = clientConnection;
        this.onlinePlayer = onlinePlayer;
    }

    public OnlinePlayer getOnlinePlayer() {
        return onlinePlayer;
    }

    public ClientConnection getClientConnection() {
        return clientConnection;
    }
}
