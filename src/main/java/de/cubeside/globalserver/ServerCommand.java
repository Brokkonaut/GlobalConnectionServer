package de.cubeside.globalserver;

import java.util.Collection;

public abstract class ServerCommand {
    private final String cmd;

    public ServerCommand(String cmd) {
        this.cmd = cmd;
    }

    public final String getCommand() {
        return cmd;
    }

    public abstract void execute(GlobalServer server, ArgsParser args);

    public Collection<String> tabComplete(GlobalServer server, ArgsParser argsParser) {
        return null;
    }
}
