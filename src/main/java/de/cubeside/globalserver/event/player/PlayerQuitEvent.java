package de.cubeside.globalserver.event.player;

import de.cubeside.globalserver.ClientConnection;
import de.cubeside.globalserver.OnlinePlayer;

public class PlayerQuitEvent extends PlayerEvent {
    public PlayerQuitEvent(ClientConnection clientConnection, OnlinePlayer onlinePlayer) {
        super(clientConnection, onlinePlayer);
    }
}
