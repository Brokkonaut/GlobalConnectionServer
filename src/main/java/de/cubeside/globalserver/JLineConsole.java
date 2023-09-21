package de.cubeside.globalserver;

import de.iani.cubesideutils.commands.ArgsParser;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class JLineConsole implements ConsoleImpl {
    private final static Logger logger = LogManager.getLogger("Console");
    private GlobalServer server;
    private SimpleConsoleReaderThread thread;
    private volatile boolean running;
    private Terminal terminal;
    private LineReader lineReader;

    public JLineConsole(GlobalServer server) {
        this.server = server;
        this.running = true;

        logger.log(Level.INFO, "Starting console...");
        try {
            terminal = TerminalBuilder.builder().build();
            lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(new DefaultHistory())
                    .completer(new ConsoleCompleter())
                    .option(Option.DISABLE_EVENT_EXPANSION, true)
                    // .variable("history-file", ".history")
                    .build();
        } catch (IOException e) {
            logger.log(Level.ERROR, "Error creating console", e);
        }

        this.thread = new SimpleConsoleReaderThread();
        this.thread.setName("console");
        this.thread.setDaemon(true);
        this.thread.start();

    }

    @Override
    public void appendOutput(String message) {
        lineReader.printAbove(message);
    }

    @Override
    public void stop() {
        running = false;
        if (terminal != null) {
            try {
                terminal.close();
            } catch (IOException e) {
                logger.error("Exception while closing the console", e);
            }
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    private class ConsoleCompleter implements Completer {
        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            if (line.wordIndex() == 0) {
                for (String commandName : server.getCommandNames()) {
                    candidates.add(new Candidate(commandName));
                }
            } else {
                List<String> words = line.words();
                ServerCommand command = server.getCommand(words.get(0));
                if (command != null) {
                    int wordCount = line.wordIndex();
                    String[] wordsArray = new String[wordCount];
                    for (int i = 0; i < wordCount; i++) {
                        wordsArray[i] = words.get(i + 1);
                    }
                    Collection<String> result;
                    server.getReadLock().lock();
                    try {
                        result = command.tabComplete(server, new ArgsParser(wordsArray));
                    } finally {
                        server.getReadLock().unlock();
                    }
                    if (result != null && !result.isEmpty()) {
                        for (String suggestion : result) {
                            candidates.add(new Candidate(suggestion));
                        }
                    }
                }
            }
        }
    }

    private class SimpleConsoleReaderThread extends Thread {
        @Override
        public void run() {
            while (running) {
                try {
                    String line = lineReader.readLine("> ");
                    server.processCommand(line == null ? "stop" : line);
                } catch (UserInterruptException e) {
                    logger.log(Level.ERROR, "User Interrupt", e);
                } catch (EndOfFileException e) {
                    running = false;
                }
            }
            // BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            // while (running) {
            // try {
            // String line = reader.readLine();
            // server.processCommand(line == null ? "stop" : line);
            // } catch (IOException e) {
            // logger.log(Level.ERROR, "Error reding from console", e);
            // }
            // }
            logger.log(Level.INFO, "Console shutdown completed.");
        }
    }
}
