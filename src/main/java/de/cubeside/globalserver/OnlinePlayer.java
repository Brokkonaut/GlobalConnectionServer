package de.cubeside.globalserver;

import java.util.UUID;

public class OnlinePlayer {
    private final UUID uuid;
    private final String name;
    private final long joinTime;

    public OnlinePlayer(UUID uuid, String name, long joinTime) {
        this.uuid = uuid;
        this.name = name;
        this.joinTime = joinTime;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public long getJoinTime() {
        return joinTime;
    }
}
