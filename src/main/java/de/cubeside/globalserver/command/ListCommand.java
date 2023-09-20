package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ClientConnection;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.OnlinePlayer;
import de.cubeside.globalserver.ServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.List;

public class ListCommand extends ServerCommand {
    public ListCommand() {
        super("list");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        List<ClientConnection> connections = server.getConnections();
        for (ClientConnection cc : connections) {
            StringBuilder sb = new StringBuilder();
            sb.append("Server ").append(cc.getAccount()).append(":");
            GlobalServer.LOGGER.info(sb.toString());
            sb = new StringBuilder();
            for (OnlinePlayer player : cc.getPlayers()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                } else {
                    sb.append("  ");
                }
                sb.append(player.getName());
            }
            if (sb.length() == 0) {
                sb.append("  (nobody)");
            }
            GlobalServer.LOGGER.info(sb.toString());
        }
        if (connections.isEmpty()) {
            GlobalServer.LOGGER.info("(no server online)");
        }
    }
}
