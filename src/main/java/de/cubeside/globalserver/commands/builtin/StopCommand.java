package de.cubeside.globalserver.commands.builtin;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.AbstractServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;

public class StopCommand extends AbstractServerCommand {
    public StopCommand() {
        super("stop");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        server.stopServer();
    }
}
