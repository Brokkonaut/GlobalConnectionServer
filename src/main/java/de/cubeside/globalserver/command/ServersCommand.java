package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ArgsParser;
import de.cubeside.globalserver.ClientConnection;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;
import java.util.List;

public class ServersCommand extends ServerCommand {
    public ServersCommand() {
        super("servers");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        StringBuilder sb = new StringBuilder();
        List<ClientConnection> connections = server.getConnections();
        sb.append("Servers (").append(connections.size()).append("): ");
        boolean first = true;
        for (ClientConnection cc : connections) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(cc.getAccount());
        }
        GlobalServer.LOGGER.info(sb.toString());
    }
}
