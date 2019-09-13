package de.cubeside.globalserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleConsole {
    private final static Logger logger = LogManager.getLogger("Console");
    private GlobalServer server;
    private SimpleConsoleReaderThread thread;
    private volatile boolean running;

    public SimpleConsole(GlobalServer server) {
        this.server = server;
        this.running = true;

        this.thread = new SimpleConsoleReaderThread();
        this.thread.setName("console");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void stop() {
        running = false;
    }

    private class SimpleConsoleReaderThread extends Thread {
        @Override
        public void run() {
            logger.log(Level.INFO, "Starting console...");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (running) {
                try {
                    String line = reader.readLine();
                    server.processCommand(line == null ? "stop" : line);
                } catch (IOException e) {
                    logger.log(Level.ERROR, "Error reding from console", e);
                }
            }
            logger.log(Level.INFO, "Console shutdown completed.");
        }
    }
}
