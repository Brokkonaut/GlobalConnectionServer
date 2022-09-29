package de.cubeside.globalserver.event.data;

import de.cubeside.globalserver.ClientConnection;
import de.cubeside.globalserver.event.clientconnection.ClientConnectionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DataForwardEvent extends ClientConnectionEvent {
    private HashSet<ClientConnection> targets;
    private String channel;
    private UUID targetUuid;
    private String targetServer;
    private byte[] data;
    private boolean allowRestricted;
    private boolean toAllUnrestrictedServers;
    private boolean cancelled = false;

    public DataForwardEvent(ClientConnection source, HashSet<ClientConnection> targets, String channel, UUID targetUuid, String targetServer, byte[] data, boolean allowRestricted, boolean toAllUnrestrictedServers) {
        super(source);
        this.targets = targets;
        this.channel = channel;
        this.targetUuid = targetUuid;
        this.targetServer = targetServer;
        this.data = data;
        this.allowRestricted = allowRestricted;
        this.toAllUnrestrictedServers = toAllUnrestrictedServers;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        this.data = data;
    }

    public Set<ClientConnection> getTargets() {
        return targets;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        this.channel = channel;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }

    public boolean isAllowRestricted() {
        return allowRestricted;
    }

    public boolean isToAllUnrestrictedServers() {
        return toAllUnrestrictedServers;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
