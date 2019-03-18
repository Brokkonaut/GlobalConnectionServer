package de.cubeside.globalserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GlobalServer {
    private final static Logger LOGGER = LogManager.getLogger("Server");

    private ServerListener listener;
    private SimpleConsole console;

    private boolean running;

    private HashMap<String, ClientConfig> clientConfigs;

    private ArrayList<ClientConnection> pendingConnections;

    private ArrayList<ClientConnection> connections;

    private final Object sync = new Object();

    public GlobalServer() {
        LOGGER.info("Starting GlobalServer...");
        clientConfigs = new HashMap<>();
        addClientConfig("test", "testpassword");
        addClientConfig("test2", "testpassword");
        addClientConfig("test3", "testpassword");
        pendingConnections = new ArrayList<>();
        connections = new ArrayList<>();
    }

    private void addClientConfig(String login, String password) {
        Objects.requireNonNull(login, "login must not be null");
        Objects.requireNonNull(password, "password must not be null");
        if (clientConfigs.containsKey(login)) {
            throw new IllegalArgumentException("Login name in use: " + login);
        }
        clientConfigs.put(login, new ClientConfig(login, password));
    }

    public void run() {
        running = true;
        int port = 12345;
        try {
            listener = new ServerListener(this, port);
            listener.start();
        } catch (IOException e) {
            LOGGER.error("Could not bind to " + port + ": " + e.getMessage());
            return;
        }
        console = new SimpleConsole(this);
        synchronized (sync) {
            while (running) {
                for (ClientConnection cc : connections) {
                    cc.sendPing();
                }
                try {
                    sync.wait(20000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    public static void main(String[] args) {
        new GlobalServer().run();
        LogManager.shutdown();
    }

    public void processCommand(String line) {
        line = line.trim();
        if (line.length() == 0) {
            return;
        }
        int firstSpace = line.indexOf(' ');
        String cmd = (firstSpace < 0 ? line : line.substring(0, firstSpace)).toLowerCase().trim();
        String args = firstSpace < 0 ? "" : line.substring(firstSpace + 1);
        synchronized (sync) {
            if (cmd.equals("stop")) {
                stopServer();
            } else if (cmd.equals("servers")) {
                StringBuilder sb = new StringBuilder();
                sb.append("Servers (").append(connections.size()).append("): ");
                boolean first = true;
                for (ClientConnection cc : connections) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append(cc.getAccount());
                }
                LOGGER.info(sb.toString());
            } else {
                LOGGER.info("Unknown command: " + cmd);
            }
        }
    }

    private void stopServer() {
        listener.shutdown();
        synchronized (sync) {
            running = false;
            sync.notify();
            for (ClientConnection cc : new ArrayList<>(pendingConnections)) {
                cc.closeConnection();
            }
            pendingConnections.clear();
            for (ClientConnection cc : new ArrayList<>(connections)) {
                cc.closeConnection();
            }
            connections.clear();
        }
        console.stop();
    }

    public void addPendingConnection(ClientConnection connection) {
        synchronized (sync) {
            pendingConnections.add(connection);
        }
    }

    public void removeConnection(ClientConnection connection) {
        synchronized (sync) {
            boolean wasOnline = connections.remove(connection);
            pendingConnections.remove(connection);
            if (wasOnline) {
                processServerOffline(connection);
            }
        }
    }

    public boolean processLogin(ClientConnection connection, String account, byte[] password, byte[] saltServer, byte[] saltClient) throws IOException {
        synchronized (sync) {
            ClientConfig config = clientConfigs.get(account);
            if (config == null || !config.checkPassword(password, saltServer, saltClient)) {
                LOGGER.info("Login failed for '" + account + "'.");
                pendingConnections.remove(connection);
                connection.sendLoginResultAndActivateEncryption(false, null);
                return false;
            }
            LOGGER.info("Login successfull for '" + account + "'.");

            for (ClientConnection cc : connections) {
                if (cc.getAccount().equals(account)) {
                    cc.closeConnection();
                    removeConnection(cc);
                    break;
                }
            }

            pendingConnections.remove(connection);
            connections.add(connection);
            connection.sendLoginResultAndActivateEncryption(true, config);
            // send online servers
            for (ClientConnection cc : connections) {
                if (cc != connection) {
                    cc.sendServerOnline(account);
                    connection.sendServerOnline(cc.getAccount());
                    Collection<OnlinePlayer> players = cc.getPlayers();
                    for (OnlinePlayer e : players) {
                        connection.sendPlayerOnline(cc.getAccount(), e.getUuid(), e.getName(), e.getJoinTime());
                    }
                }
            }
            return true;
        }
    }

    public void processPlayerOnline(ClientConnection connection, UUID uuid, String name, long joinTime) {
        synchronized (sync) {
            if (connection.addPlayer(uuid, name, joinTime)) {
                for (ClientConnection cc : connections) {
                    if (cc != connection) {
                        cc.sendPlayerOnline(connection.getAccount(), uuid, name, joinTime);
                    }
                }
            }
        }
    }

    public void processPlayerOffline(ClientConnection connection, UUID uuid) {
        synchronized (sync) {
            if (connection.removePlayer(uuid)) {
                for (ClientConnection cc : connections) {
                    if (cc != connection) {
                        cc.sendPlayerOffline(connection.getAccount(), uuid);
                    }
                }
            }
        }
    }

    private void processServerOffline(ClientConnection connection) {
        for (ClientConnection cc : connections) {
            if (cc != connection) {
                cc.sendServerOffline(connection.getAccount());
            }
        }
    }

    public void processData(ClientConnection connection, String channel, UUID targetUuid, String targetServer, byte[] data) {
        for (ClientConnection cc : connections) {
            if (cc != connection) {
                if (targetServer == null || cc.getAccount().equals(targetServer)) {
                    if (targetUuid == null || cc.hasPlayer(targetUuid)) {
                        cc.sendData(connection.getAccount(), channel, targetUuid, targetServer, data);
                    }
                }
            }
        }
    }
}
