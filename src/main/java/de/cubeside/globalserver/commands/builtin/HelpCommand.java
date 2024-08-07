package de.cubeside.globalserver.commands.builtin;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.AbstractServerCommand;
import de.cubeside.globalserver.commands.ServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.Collection;

public class HelpCommand extends AbstractServerCommand {
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
