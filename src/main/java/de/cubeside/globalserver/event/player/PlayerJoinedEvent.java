package de.cubeside.globalserver.event.player;

import de.cubeside.globalserver.ClientConnection;
import de.cubeside.globalserver.OnlinePlayer;

public class PlayerJoinedEvent extends PlayerEvent {
    public PlayerJoinedEvent(ClientConnection clientConnection, OnlinePlayer onlinePlayer) {
        super(clientConnection, onlinePlayer);
    }
}
