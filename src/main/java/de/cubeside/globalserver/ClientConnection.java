package de.cubeside.globalserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientConnection extends Thread {
    protected static final Logger LOGGER = LogManager.getLogger("Client");

    private final SecureRandom random = new SecureRandom();
    private final GlobalServer server;
    private byte[] randomNumberServer;
    private byte[] randomNumberClient;

    private final Socket socket;
    private OutputStream socketos;
    private DataOutputStream os;
    private InputStream socketis;
    private DataInputStream is;

    private String account;
    private ClientConfig client;
    private HashMap<UUID, OnlinePlayer> playersOnline;
    private final Object sendSync = new Object();

    public ClientConnection(GlobalServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
        this.playersOnline = new HashMap<>();
        try {
            socket.setSoTimeout(30000);
        } catch (SocketException e) {
            LOGGER.error("Could not set socket timeout!");
        }
    }

    @Override
    public void run() {
        LOGGER.info("Processing login request from " + socket.getInetAddress().getHostAddress());
        randomNumberServer = new byte[32];
        randomNumberClient = new byte[32];
        random.nextBytes(randomNumberServer);
        try {
            socketos = socket.getOutputStream();
            socketis = socket.getInputStream();
            os = new DataOutputStream(socketos);
            is = new DataInputStream(socketis);
            os.write(randomNumberServer);
            is.readFully(randomNumberClient);
            account = is.readUTF();
            byte[] password = new byte[32];
            is.readFully(password);
            ClientConfig result = server.processLogin(this, account, password, randomNumberServer, randomNumberClient);
            if (result == null) {
                LOGGER.info("Login for '" + account + "' failed from " + socket.getInetAddress().getHostAddress());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    // ignored
                }
                return; // finished
            }
            // client = result;

            while (true) {
                ClientPacketType packetType = ClientPacketType.valueOf(is.readByte());
                // LOGGER.info("Packet received from " + account + ": " + packetType);
                switch (packetType) {
                    case PING: {
                        sendPong();
                        break;
                    }
                    case PONG: {
                        break;
                    }
                    case PLAYER_ONLINE: {
                        long mostSigBits = is.readLong();
                        long leastSigBits = is.readLong();
                        UUID uuid = new UUID(mostSigBits, leastSigBits);
                        String name = is.readUTF();
                        long joinTime = is.readLong();
                        server.processPlayerOnline(this, uuid, name, joinTime);
                        break;
                    }
                    case PLAYER_OFFLINE: {
                        long mostSigBits = is.readLong();
                        long leastSigBits = is.readLong();
                        UUID uuid = new UUID(mostSigBits, leastSigBits);
                        server.processPlayerOffline(this, uuid);
                        break;
                    }
                    case SERVER_OFFLINE: {
                        LOGGER.info("Connection for '" + account + "' from " + socket.getInetAddress().getHostAddress() + " closed remotely.");
                        server.removeConnection(this);
                        socket.close();
                        return;
                    }
                    case DATA: {
                        String channel = is.readUTF();
                        int flags = is.readByte();
                        UUID targetUuid = null;
                        if ((flags & 0x01) != 0) {
                            long mostSigBits = is.readLong();
                            long leastSigBits = is.readLong();
                            targetUuid = new UUID(mostSigBits, leastSigBits);
                        }
                        String targetServer = null;
                        if ((flags & 0x02) != 0) {
                            targetServer = is.readUTF();
                        }
                        boolean allowRestricted = false;
                        if ((flags & 0x04) != 0) {
                            allowRestricted = true;
                        }
                        boolean toAllUnrestrictedServers = false;
                        if ((flags & 0x08) != 0) {
                            toAllUnrestrictedServers = true;
                        }
                        int dataSize = is.readInt();
                        if (dataSize > 10_000_000 || dataSize < 0) {
                            // 10 mb
                            LOGGER.info("Oversized data packet received from '" + account + "' from " + socket.getInetAddress().getHostAddress() + " (" + dataSize + " bytes).");
                            socket.close();
                            break;
                        }
                        byte[] data = new byte[dataSize];
                        is.readFully(data);
                        server.processData(this, channel, targetUuid, targetServer, data, allowRestricted, toAllUnrestrictedServers);
                        break;
                    }
                }
            }
        } catch (Throwable e) {
            server.removeConnection(this);
            try {
                socket.close();
            } catch (IOException e2) {
                // ignore
            }
            if (e instanceof SocketTimeoutException) {
                LOGGER.info("Connection for '" + account + "' from " + socket.getInetAddress().getHostAddress() + " timed out.");
            } else if (e instanceof IOException) {
                LOGGER.info("Connection for '" + account + "' from " + socket.getInetAddress().getHostAddress() + " closed: " + e.getMessage());
            } else {
                LOGGER.error("Exception in handler thread for '" + account + "' from " + socket.getInetAddress().getHostAddress() + ".", e);
            }
        }
    }

    public SecretKey generateSecretKey(SecureRandom random) {
        KeyGenerator keygeneratorAES;
        try {
            keygeneratorAES = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("No AES?", e);
        }
        keygeneratorAES.init(128, random);
        return keygeneratorAES.generateKey();
    }

    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            // ignored
        }
    }

    public String getAccount() {
        return account;
    }

    public ClientConfig getClient() {
        return client;
    }

    void setClient(ClientConfig client) {
        this.client = client;
    }

    public OnlinePlayer addPlayer(UUID uuid, String name, long joinTime) {
        if (playersOnline.containsKey(uuid)) {
            return null;
        }
        OnlinePlayer joined = new OnlinePlayer(uuid, name, joinTime);
        playersOnline.put(uuid, joined);
        return joined;
    }

    public OnlinePlayer removePlayer(UUID uuid) {
        return playersOnline.remove(uuid);
    }

    public boolean hasPlayer(UUID uuid) {
        return playersOnline.containsKey(uuid);
    }

    public Collection<OnlinePlayer> getPlayers() {
        return playersOnline.values();
    }

    public void sendLoginResultAndActivateEncryption(boolean success, ClientConfig config) throws IOException {
        os.writeByte(success ? 0 : 1);

        byte[] secret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(randomNumberServer);
            if (config != null) {
                digest.update(config.getPassword().getBytes(StandardCharsets.UTF_8));
            }
            digest.update(randomNumberClient);
            secret = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);// impossible
        }

        SecureRandom secureRandom = new SecureRandom();
        SecretKey kpOut = generateSecretKey(secureRandom);
        SecretKey kpIn = generateSecretKey(secureRandom);
        byte[] out = new byte[32];
        byte[] encoded = kpOut.getEncoded();
        for (int i = 0; i < 16; i++) {
            out[i] = (byte) (secret[i] ^ encoded[i]);
        }
        encoded = kpIn.getEncoded();
        for (int i = 0; i < 16; i++) {
            out[i + 16] = (byte) (secret[i + 16] ^ encoded[i]);
        }
        os.write(out);

        try {
            Cipher cipherAESout = Cipher.getInstance("AES/CFB8/NoPadding");
            cipherAESout.init(Cipher.ENCRYPT_MODE, kpOut, new IvParameterSpec(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));

            os = new DataOutputStream(new CipherOutputStream(new BufferedOutputStream(socketos), cipherAESout));

            Cipher cipherAESin = Cipher.getInstance("AES/CFB8/NoPadding");
            cipherAESin.init(Cipher.DECRYPT_MODE, kpIn, new IvParameterSpec(new byte[] { 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 }));

            is = new DataInputStream(new CipherInputStream(new BufferedInputStream(socketis), cipherAESin));
        } catch (GeneralSecurityException e) {
            throw new Error(e);// impossible?
        }
    }

    public void sendPing() {
        synchronized (sendSync) {
            if (os != null) {
                try {
                    os.writeByte(ServerPacketType.PING.ordinal());
                    os.flush();
                } catch (IOException e) {
                    LOGGER.error("Could not send PING");
                }
            }
        }
    }

    private void sendPong() {
        synchronized (sendSync) {
            if (os != null) {
                try {
                    os.writeByte(ServerPacketType.PONG.ordinal());
                    os.flush();
                } catch (IOException e) {
                    LOGGER.error("Could not send PONG");
                }
            }
        }
    }

    public void sendServerOnline(String server) {
        synchronized (sendSync) {
            if (os != null) {
                try {
                    os.writeByte(ServerPacketType.SERVER_ONLINE.ordinal());
                    os.writeUTF(server);
                    os.flush();
                    // LOGGER.info("Packet sent to " + account + ": SERVER_ONLINE");
                } catch (IOException e) {
                    LOGGER.error("Could not send SERVER_ONLINE");
                }
            }
        }
    }

    public void sendServerOffline(String server) {
        synchronized (sendSync) {
            if (os != null) {
                try {
                    os.writeByte(ServerPacketType.SERVER_OFFLINE.ordinal());
                    os.writeUTF(server);
                    os.flush();
                } catch (IOException e) {
                    LOGGER.error("Could not send SERVER_OFFLINE");
                }
            }
        }
    }

    public void sendPlayerOnline(String server, UUID uuid, String name, long joinTime) {
        synchronized (sendSync) {
            if (os != null) {
                try {
                    os.writeByte(ServerPacketType.PLAYER_ONLINE.ordinal());
                    os.writeUTF(server);
                    os.writeLong(uuid.getMostSignificantBits());
                    os.writeLong(uuid.getLeastSignificantBits());
                    os.writeUTF(name);
                    os.writeLong(joinTime);
                    os.flush();
                    // LOGGER.info("Packet sent to " + account + ": PLAYER_ONLINE");
                } catch (IOException e) {
                    LOGGER.error("Could not send PLAYER_ONLINE");
                }
            }
        }
    }

    public void sendPlayerOffline(String server, UUID uuid) {
        synchronized (sendSync) {
            if (os != null) {
                try {
                    os.writeByte(ServerPacketType.PLAYER_OFFLINE.ordinal());
                    os.writeUTF(server);
                    os.writeLong(uuid.getMostSignificantBits());
                    os.writeLong(uuid.getLeastSignificantBits());
                    os.flush();
                } catch (IOException e) {
                    LOGGER.error("Could not send PLAYER_OFFLINE");
                }
            }
        }
    }

    public void sendData(ClientConnection fromServer, String channel, UUID targetUuid, ClientConnection targetServer, byte[] data) {
        if (fromServer == null) {
            throw new NullPointerException("fromServer");
        }
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        synchronized (sendSync) {
            if (os != null) {
                try {
                    os.writeByte(ServerPacketType.DATA.ordinal());
                    os.writeUTF(fromServer.getAccount());
                    os.writeUTF(channel);
                    int flags = (targetUuid != null ? 1 : 0) + (targetServer != null ? 2 : 0);
                    os.writeByte(flags);
                    if (targetUuid != null) {
                        os.writeLong(targetUuid.getMostSignificantBits());
                        os.writeLong(targetUuid.getLeastSignificantBits());
                    }
                    if (targetServer != null) {
                        os.writeUTF(targetServer.getAccount());
                    }
                    os.writeInt(data.length);
                    os.write(data);
                    os.flush();
                    // LOGGER.info("Packet sent to " + account + ": DATA");
                } catch (IOException e) {
                    LOGGER.error("Could not send DATA");
                }
            }
        }
    }

    public OnlinePlayer getPlayer(UUID uuid) {
        return playersOnline.get(uuid);
    }
}
