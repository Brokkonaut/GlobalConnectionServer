package de.cubeside.globalserver.command;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.Collection;

public class HelpCommand extends ServerCommand {
    public HelpCommand() {
        super("help");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        Collection<ServerCommand> commands = server.getCommands();
        StringBuilder sb = new StringBuilder();
        for (ServerCommand cc : commands) {
            if (sb.length() > 0) {
                sb.append(", ");
            } else {
                sb.append("Commands: ");
            }
            sb.append(cc.getCommand());
        }
        if (sb.length() == 0) {
            sb.append("Commands: (none)");
        }
        GlobalServer.LOGGER.info(sb.toString());
    }
}
