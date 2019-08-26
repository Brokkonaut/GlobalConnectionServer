package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ArgsParser;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;

public class StopCommand extends ServerCommand {
    public StopCommand() {
        super("stop");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        server.stopServer();
    }
}
