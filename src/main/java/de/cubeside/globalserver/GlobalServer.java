package de.cubeside.globalserver;

import de.cubeside.globalserver.command.AccountAddAllowedChannelCommand;
import de.cubeside.globalserver.command.AccountInfoCommand;
import de.cubeside.globalserver.command.AccountRemoveAllowedChannelCommand;
import de.cubeside.globalserver.command.AccountSetPasswordCommand;
import de.cubeside.globalserver.command.AccountSetRestrictedCommand;
import de.cubeside.globalserver.command.AccountsCommand;
import de.cubeside.globalserver.command.CreateAccountCommand;
import de.cubeside.globalserver.command.HelpCommand;
import de.cubeside.globalserver.command.ListCommand;
import de.cubeside.globalserver.command.ServersCommand;
import de.cubeside.globalserver.command.StopCommand;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class GlobalServer {
    public final static Logger LOGGER = LogManager.getLogger("Server");

    private ServerListener listener;
    private SimpleConsole console;

    private boolean running;

    private File configFile = new File("config.yml");
    private Yaml configYaml;
    private ServerConfig serverConfig;

    private HashMap<String, ClientConfig> clientConfigs;

    private ArrayList<ClientConnection> pendingConnections;

    private ArrayList<ClientConnection> connections;

    private HashMap<String, ServerCommand> commands;

    private final Object sync = new Object();

    public GlobalServer() {
        LOGGER.info("Starting GlobalServer...");

        Constructor constructor = new Constructor(ServerConfig.class);
        TypeDescription serverConfigDescription = new TypeDescription(ServerConfig.class);
        serverConfigDescription.addPropertyParameters("clientConfigs", ClientConfig.class);
        constructor.addTypeDescription(serverConfigDescription);
        configYaml = new Yaml(constructor);

        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), Charset.forName("UTF-8")))) {
                serverConfig = configYaml.loadAs(reader, ServerConfig.class);
                saveConfig();
            } catch (Exception e) {
                LOGGER.error("Could not parse config!", e);
            }
        }
        if (serverConfig == null) {
            serverConfig = new ServerConfig();
            LOGGER.info("Generating new config!");
            if (!configFile.exists()) {
                saveConfig();
            }
        }
        clientConfigs = new HashMap<>();
        for (ClientConfig cc : serverConfig.getClientConfigs()) {
            clientConfigs.put(cc.getLogin(), cc);
        }
        pendingConnections = new ArrayList<>();
        connections = new ArrayList<>();
        commands = new HashMap<>();

        addCommand(new HelpCommand());
        addCommand(new StopCommand());
        addCommand(new ServersCommand());
        addCommand(new ListCommand());
        addCommand(new AccountsCommand());
        addCommand(new AccountInfoCommand());
        addCommand(new CreateAccountCommand());
        addCommand(new AccountSetPasswordCommand());
        addCommand(new AccountSetRestrictedCommand());
        addCommand(new AccountAddAllowedChannelCommand());
        addCommand(new AccountRemoveAllowedChannelCommand());
    }

    public Collection<ServerCommand> getCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    private void addCommand(ServerCommand command) {
        commands.put(command.getCommand().toLowerCase().trim(), command);
    }

    public void addAccount(String login, String password) {
        Objects.requireNonNull(login, "login must not be null");
        Objects.requireNonNull(password, "password must not be null");
        if (clientConfigs.containsKey(login)) {
            throw new IllegalArgumentException("Login name in use: " + login);
        }
        ClientConfig cfg = new ClientConfig(login, password, false, new HashSet<>());
        clientConfigs.put(login, cfg);
        serverConfig.getClientConfigs().add(cfg);
        saveConfig();
    }

    public void saveConfig() {
        String output = configYaml.dumpAsMap(serverConfig);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), Charset.forName("UTF-8")))) {
            writer.write(output);
        } catch (Exception e) {
            LOGGER.error("Could not save config!", e);
        }
    }

    public Collection<ClientConfig> getAccounts() {
        return Collections.unmodifiableCollection(clientConfigs.values());
    }

    public ClientConfig getAccount(String name) {
        return clientConfigs.get(name);
    }

    public List<ClientConnection> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    public void run() {
        running = true;
        int port = serverConfig.getPort();
        if (port <= 0) {
            port = 25701;
            serverConfig.setPort(port);
            saveConfig();
        }
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
            ServerCommand command = commands.get(cmd);
            if (command != null) {
                ArrayList<String> splitArgs = new ArrayList<>();
                for (String s : args.trim().split(" ++")) {
                    String s2 = s.trim();
                    if (s2.length() > 0) {
                        splitArgs.add(s2);
                    }
                }
                command.execute(this, new ArgsParser(splitArgs.toArray(new String[splitArgs.size()])));
            } else {
                LOGGER.info("Unknown command: " + cmd);
            }
        }
    }

    public void stopServer() {
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

    public ClientConfig processLogin(ClientConnection connection, String account, byte[] password, byte[] saltServer, byte[] saltClient) throws IOException {
        synchronized (sync) {
            ClientConfig config = clientConfigs.get(account);
            if (config == null || !config.checkPassword(password, saltServer, saltClient)) {
                LOGGER.info("Login failed for '" + account + "'.");
                pendingConnections.remove(connection);
                connection.sendLoginResultAndActivateEncryption(false, null);
                return null;
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
            return config;
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

    public void processData(ClientConnection connection, String channel, UUID targetUuid, String targetServer, byte[] data, boolean allowRestricted, boolean toAllUnrestrictedServers) {
        boolean fromRestricted = connection.getClient().isRestricted();
        if (!fromRestricted || connection.getClient().getAllowedChannels().contains(channel)) {
            for (ClientConnection cc : connections) {
                if (cc != connection) {
                    boolean toRestricted = cc.getClient().isRestricted();
                    if (!toRestricted || cc.getClient().getAllowedChannels().contains(channel)) {
                        boolean explicitServer = targetServer != null && cc.getAccount().equals(targetServer);
                        if (targetServer == null || explicitServer) {
                            boolean explicitPlayer = targetUuid != null && cc.hasPlayer(targetUuid);
                            if (toAllUnrestrictedServers || targetUuid == null || explicitPlayer) {
                                if (allowRestricted || (!fromRestricted && !toRestricted) || explicitServer || explicitPlayer) {
                                    cc.sendData(connection.getAccount(), channel, targetUuid, targetServer, data);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
