package de.cubeside.globalserver;

import java.util.ArrayList;

public class ServerConfig {
    private int port = 25701;
    private ArrayList<ClientConfig> clientConfigs = new ArrayList<>();

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ArrayList<ClientConfig> getClientConfigs() {
        return clientConfigs;
    }

    public void setClientConfigs(ArrayList<ClientConfig> clientConfigs) {
        this.clientConfigs = clientConfigs;
    }
}
