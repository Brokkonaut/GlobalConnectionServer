package de.cubeside.globalserver;

import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.builtin.HelpCommand;
import de.cubeside.globalserver.commands.builtin.ListCommand;
import de.cubeside.globalserver.commands.builtin.PluginsCommand;
import de.cubeside.globalserver.commands.builtin.ServersCommand;
import de.cubeside.globalserver.commands.builtin.StopCommand;
import de.cubeside.globalserver.commands.builtin.account.AccountCommand;
import de.cubeside.globalserver.commands.builtin.permissions.PermissionGroupCommand;
import de.cubeside.globalserver.event.EventBus;
import de.cubeside.globalserver.event.clientconnection.ClientConnectionDissolveEvent;
import de.cubeside.globalserver.event.clientconnection.ClientConnectionEstablishedEvent;
import de.cubeside.globalserver.event.data.DataForwardEvent;
import de.cubeside.globalserver.event.globalserver.GlobalServerStartedEvent;
import de.cubeside.globalserver.event.globalserver.GlobalServerStoppedEvent;
import de.cubeside.globalserver.event.globalserver.GlobalServerStoppingEvent;
import de.cubeside.globalserver.event.player.PlayerJoinedEvent;
import de.cubeside.globalserver.event.player.PlayerQuitEvent;
import de.cubeside.globalserver.permissions.GlobalPermissions;
import de.cubeside.globalserver.permissions.impl.PermissionUser;
import de.cubeside.globalserver.plugin.Plugin;
import de.cubeside.globalserver.plugin.PluginLoadException;
import de.cubeside.globalserver.plugin.PluginManager;
import de.cubeside.globalserver.plugin.PluginManagerWrapper;
import de.iani.cubesideutils.commands.ArgsParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class GlobalServer {
    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        PrintStream logger = IoBuilder.forLogger("System.out").setLevel(Level.INFO).buildPrintStream();
        PrintStream errorLogger = IoBuilder.forLogger("System.err").setLevel(Level.ERROR).buildPrintStream();
        System.setOut(logger);
        System.setErr(errorLogger);
    }
    public final static Logger LOGGER = LogManager.getLogger("Server");

    private ServerListener listener;
    private static ConsoleImpl console;

    private boolean running;

    private File configFile = new File("config.yml");
    private Yaml configYaml;
    private ServerConfig serverConfig;

    private ConcurrentHashMap<String, ClientConfig> clientConfigs;

    private ArrayList<ClientConnection> pendingConnections;

    private ArrayList<ClientConnection> connections;
    private HashMap<String, ClientConnection> connectionsByAccount;

    private ConcurrentHashMap<String, ServerCommand> commands;

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    /**
     * Required when a command is executed/tab completed or data is sent
     */
    private final Lock readLock = readWriteLock.readLock();
    /**
     * Required when a server is added/removed or a player is added/removed
     */
    private final Lock writeLock = readWriteLock.writeLock();
    /**
     * Required when data is modified and there is no writeLock. It has to be aquired under readLock and is not allowed to aquire other locks from there
     */
    private final Lock dataLock = new ReentrantLock();
    /**
     * Used when accessing the field "running" and when waiting for/signaling the shutdownCondition
     */
    private final Lock shutdownLock = new ReentrantLock();
    private final Condition shutdownCondition = shutdownLock.newCondition();

    private final EventBus eventBus = new EventBus();

    private final PluginManagerWrapper pluginManagerWrapper;
    private final PluginManager pluginManager;

    private File pluginFolder;

    private final ExecutorService executor;

    private final GlobalPermissions globalPermissions;

    public GlobalServer() throws PluginLoadException {
        console = new JLineConsole(this);

        LOGGER.info("Starting GlobalServer...");

        executor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 300L, TimeUnit.SECONDS, new SynchronousQueue<>());
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
        loaderOptions.setNestingDepthLimit(Integer.MAX_VALUE);
        Constructor constructor = new Constructor(ServerConfig.class, loaderOptions);
        TypeDescription serverConfigDescription = new TypeDescription(ServerConfig.class);
        serverConfigDescription.addPropertyParameters("clientConfigs", ClientConfig.class);
        constructor.addTypeDescription(serverConfigDescription);
        TypeDescription clientConfigDescription = new TypeDescription(ClientConfig.class);
        clientConfigDescription.addPropertyParameters("allowedChannels", String.class);
        constructor.addTypeDescription(clientConfigDescription);
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
        clientConfigs = new ConcurrentHashMap<>();
        for (ClientConfig cc : serverConfig.getClientConfigs()) {
            clientConfigs.put(cc.getLogin(), cc);
        }
        pendingConnections = new ArrayList<>();
        connections = new ArrayList<>();
        connectionsByAccount = new HashMap<>();
        commands = new ConcurrentHashMap<>();
        globalPermissions = new GlobalPermissions(this);

        addCommand(new HelpCommand());
        addCommand(new StopCommand());
        addCommand(new ServersCommand());
        addCommand(new ListCommand());
        addCommand(new PluginsCommand());
        addCommand(new AccountCommand());
        addCommand(new PermissionGroupCommand());

        this.pluginFolder = new File("./plugins");
        pluginFolder.mkdirs();
        pluginManagerWrapper = new PluginManagerWrapper(this);
        pluginManager = pluginManagerWrapper.getPluginManager();
        pluginManagerWrapper.loadPlugins();
        for (Plugin plugin : pluginManager.getPlugins()) {
            LOGGER.info("Starting plugin " + plugin.getDescription().getName() + " " + plugin.getDescription().getVersion());
            try {
                plugin.onLoad();
            } catch (Throwable t) {
                LOGGER.error("Exception while starting plugin " + plugin.getDescription().getName(), t);
            }
        }
    }

    public File getPluginFolder() {
        return pluginFolder;
    }

    public Collection<ServerCommand> getCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    public Collection<String> getCommandNames() {
        return Collections.unmodifiableCollection(commands.keySet());
    }

    public ServerCommand getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    public void addCommand(ServerCommand command) {
        addCommand(command.getCommand(), command);
    }

    void addCommand(String commandString, ServerCommand command) {
        commands.put(commandString.toLowerCase().trim(), command);
    }

    public void addAccount(String login, String password) {
        dataLock.lock();
        try {
            Objects.requireNonNull(login, "login must not be null");
            Objects.requireNonNull(password, "password must not be null");
            if (clientConfigs.containsKey(login)) {
                throw new IllegalArgumentException("Login name in use: " + login);
            }
            ClientConfig cfg = new ClientConfig(login, password, false, new HashSet<>());
            clientConfigs.put(login, cfg);
            serverConfig.getClientConfigs().add(cfg);
            saveConfig();
        } finally {
            dataLock.unlock();
        }
    }

    public void removeAccount(String login) {
        dataLock.lock();
        try {
            Objects.requireNonNull(login, "login must not be null");
            if (!clientConfigs.containsKey(login)) {
                throw new IllegalArgumentException("Account does not exist: " + login);
            }
            ClientConnection connection = connectionsByAccount.get(login);
            if (connection != null) {
                connection.closeConnection();
            }
            clientConfigs.remove(login);
            serverConfig.getClientConfigs().removeIf(cfg -> cfg.getLogin().equals(login));
            saveConfig();
        } finally {
            dataLock.unlock();
        }
    }

    public void saveConfig() {
        dataLock.lock();
        try {
            String output = configYaml.dumpAsMap(serverConfig);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), Charset.forName("UTF-8")))) {
                writer.write(output);
            } catch (Exception e) {
                LOGGER.error("Could not save config!", e);
            }
        } finally {
            dataLock.unlock();
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

    public ClientConnection getConnection(String account) {
        return connectionsByAccount.get(account);
    }

    void run() {
        running = true;
        readLock.lock();
        try {
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
            getEventBus().dispatchEvent(new GlobalServerStartedEvent(this));
        } finally {
            readLock.unlock();
        }
        while (true) {
            readLock.lock();
            try {
                for (ClientConnection cc : connections) {
                    cc.sendPing();
                }
            } finally {
                readLock.unlock();
            }
            shutdownLock.lock();
            try {
                if (running) {
                    try {
                        shutdownCondition.await(20, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
                if (!running) {
                    break;
                }
            } finally {
                shutdownLock.unlock();
            }
        }
        writeLock.lock();
        try {
            listener.shutdown();

            getEventBus().dispatchEvent(new GlobalServerStoppingEvent(this));

            for (ClientConnection cc : new ArrayList<>(pendingConnections)) {
                cc.closeConnection();
            }
            pendingConnections.clear();
            for (ClientConnection cc : new ArrayList<>(connections)) {
                cc.closeConnection();
            }
            connections.clear();
            connectionsByAccount.clear();

            getEventBus().dispatchEvent(new GlobalServerStoppedEvent(this));

            for (Plugin plugin : pluginManager.getPlugins()) {
                LOGGER.info("Unloading plugin " + plugin.getDescription().getName() + " " + plugin.getDescription().getVersion());
                try {
                    plugin.onUnload();
                } catch (Throwable t) {
                    LOGGER.error("Exception while unloading plugin " + plugin.getDescription().getName(), t);
                }
            }
            executor.shutdown();
            try {
                // wait 1 second without info, only print a message if this was not enough
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    LOGGER.info("Waiting 60 seconds for all async tasks to finish...");
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        LOGGER.warn("Not all tasks were completed before unloading!");
                    } else {
                        LOGGER.info("All tasks have finished executing!");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pluginManagerWrapper.shutdown();

            console.stop();
        } finally {
            writeLock.unlock();
        }
    }

    public static void main(String[] args) {
        try {
            new GlobalServer().run();
        } catch (PluginLoadException e) {
            LOGGER.error("Could not load plugins", e);
        }
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
        ServerCommand command = commands.get(cmd);
        if (command != null) {
            ArrayList<String> splitArgs = new ArrayList<>();
            for (String s : args.trim().split(" ++")) {
                String s2 = s.trim();
                if (s2.length() > 0) {
                    splitArgs.add(s2);
                }
            }
            readLock.lock();
            try {
                command.execute(this, new ArgsParser(splitArgs.toArray(new String[splitArgs.size()])));
            } catch (Throwable t) {
                LOGGER.error("Could not execute command " + line, t);
            } finally {
                readLock.unlock();
            }
        } else {
            LOGGER.info("Unknown command: " + cmd);
        }
    }

    public void stopServer() {
        shutdownLock.lock();
        try {
            running = false;
            shutdownCondition.signal();
        } finally {
            shutdownLock.unlock();
        }
    }

    void addPendingConnection(ClientConnection connection) {
        writeLock.lock();
        try {
            pendingConnections.add(connection);
        } finally {
            writeLock.unlock();
        }
    }

    void removeConnection(ClientConnection connection) {
        writeLock.lock();
        try {
            boolean wasOnline = connections.remove(connection);
            pendingConnections.remove(connection);
            if (wasOnline) {
                String account = connection.getAccount();
                connectionsByAccount.remove(account);
                for (ClientConnection cc : connections) {
                    if (cc != connection) {
                        cc.sendServerOffline(account);
                    }
                }
                for (OnlinePlayer player : connection.getPlayers()) {
                    getEventBus().dispatchEvent(new PlayerQuitEvent(connection, player));
                }
                getEventBus().dispatchEvent(new ClientConnectionDissolveEvent(connection));
                globalPermissions.getPermissionSystem().unloadUser(account);
            }
        } finally {
            writeLock.unlock();
        }
    }

    ClientConfig processLogin(ClientConnection connection, String account, byte[] password, byte[] saltServer, byte[] saltClient) throws IOException {
        writeLock.lock();
        try {
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

            connection.setClient(config);

            pendingConnections.remove(connection);
            connections.add(connection);
            connectionsByAccount.put(connection.getAccount(), connection);

            // perms
            reloadGroupsForAccount(account);

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
            getEventBus().dispatchEvent(new ClientConnectionEstablishedEvent(connection));
            return config;
        } finally {
            writeLock.unlock();
        }
    }

    void processPlayerOnline(ClientConnection connection, UUID uuid, String name, long joinTime) {
        writeLock.lock();
        try {
            OnlinePlayer joined = connection.addPlayer(uuid, name, joinTime);
            if (joined != null) {
                for (ClientConnection cc : connections) {
                    if (cc != connection) {
                        cc.sendPlayerOnline(connection.getAccount(), uuid, name, joinTime);
                    }
                }
                getEventBus().dispatchEvent(new PlayerJoinedEvent(connection, joined));
            }
        } finally {
            writeLock.unlock();
        }
    }

    void processPlayerOffline(ClientConnection connection, UUID uuid) {
        writeLock.lock();
        try {
            OnlinePlayer player = connection.removePlayer(uuid);
            if (player != null) {
                for (ClientConnection cc : connections) {
                    if (cc != connection) {
                        cc.sendPlayerOffline(connection.getAccount(), uuid);
                    }
                }
                getEventBus().dispatchEvent(new PlayerQuitEvent(connection, player));
            }
        } finally {
            writeLock.unlock();
        }
    }

    void processData(ClientConnection connection, String channel, UUID targetUuid, String targetServer, byte[] data, boolean allowRestricted, boolean toAllUnrestrictedServers) {
        readLock.lock();
        try {
            HashSet<ClientConnection> targets = new HashSet<>();
            ClientConnection targetServerConnection = targetServer == null ? null : connectionsByAccount.get(targetServer);
            boolean fromRestricted = connection.getClient().isRestricted();
            if (!fromRestricted || connection.getClient().getAllowedChannels().contains(channel)) {
                for (ClientConnection cc : connections) {
                    if (cc != connection) {
                        boolean toRestricted = cc.getClient().isRestricted();
                        if (!toRestricted || cc.getClient().getAllowedChannels().contains(channel)) {
                            boolean explicitServer = cc == targetServerConnection;
                            if (targetServer == null || explicitServer) {
                                boolean explicitPlayer = targetUuid != null && cc.hasPlayer(targetUuid);
                                if (toAllUnrestrictedServers || targetUuid == null || explicitPlayer) {
                                    if (allowRestricted || (!fromRestricted && !toRestricted) || explicitServer || explicitPlayer) {
                                        targets.add(cc);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            DataForwardEvent event = new DataForwardEvent(connection, targets, channel, targetUuid, targetServerConnection, data, allowRestricted, toAllUnrestrictedServers);
            getEventBus().dispatchEvent(event);
            if (!event.isCancelled()) {
                channel = event.getChannel();
                targetUuid = event.getTargetUuid();
                targetServerConnection = event.getTargetServer();

                data = event.getData();
                for (ClientConnection target : event.getTargets()) {
                    target.sendData(connection, channel, targetUuid, targetServerConnection, data);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public void runWithReadLock(Runnable r) {
        readLock.lock();
        try {
            r.run();
        } finally {
            readLock.unlock();
        }
    }

    public static Console getConsole() {
        return console;
    }

    Lock getReadLock() {
        return readLock;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public GlobalPermissions getGlobalPermissions() {
        return globalPermissions;
    }

    public void reloadGroupsForAccount(String accountName) {
        readLock.lock();
        try {
            ClientConnection connection = getConnection(accountName);
            if (connection != null && connection.getClient() != null) {
                ArrayList<String> groups = new ArrayList<>(connection.getClient().getGroups());
                PermissionUser user = globalPermissions.getPermissionSystem().createOrEditUser(accountName, editor -> {
                    editor.removeAllPermissions();
                    for (String group : groups) {
                        editor.addPermissionToUser("group." + group, true);
                    }
                });
                connection.setPermissions(user);
            }
        } finally {
            readLock.unlock();
        }
    }
}
