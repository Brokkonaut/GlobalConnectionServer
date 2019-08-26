package de.cubeside.globalserver;

public abstract class ServerCommand {
    private final String cmd;

    public ServerCommand(String cmd) {
        this.cmd = cmd;
    }

    public final String getCommand() {
        return cmd;
    }

    public abstract void execute(GlobalServer server, ArgsParser args);
}
