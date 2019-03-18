package de.cubeside.globalserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerListener extends Thread {

    protected static final Logger LOGGER = LogManager.getLogger("Listener");

    private ServerSocket socket;

    private volatile boolean running;

    private GlobalServer server;

    public ServerListener(GlobalServer server, int port) throws IOException {
        this.server = server;
        LOGGER.info("Opening socket on port " + port);
        socket = new ServerSocket(port);
        running = true;
        setName("listener");
    }

    @Override
    public void run() {
        LOGGER.info("Listener running...");
        while (running) {
            try {
                Socket accepted = socket.accept();
                ClientConnection con = new ClientConnection(server, accepted);
                server.addPendingConnection(con);
                con.start();
            } catch (IOException e) {
                if (!running) {
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        // ignored
                    }
                    LOGGER.info("Listener shutdown completed.");
                    return;
                }
                LOGGER.error("Exception in accept()", e);
            }
        }
    }

    public void shutdown() {
        LOGGER.info("Initiating listener shutdown...");
        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            // ignored
        }
    }
}
