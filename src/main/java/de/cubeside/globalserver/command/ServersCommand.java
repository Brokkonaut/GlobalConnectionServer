package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ClientConnection;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.AbstractServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import java.util.List;

public class ServersCommand extends AbstractServerCommand {
    public ServersCommand() {
        super("servers");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        StringBuilder sb = new StringBuilder();
        List<ClientConnection> connections = server.getConnections();
        sb.append("Servers (").append(connections.size()).append("): ");
        boolean first = true;
        ArrayList<String> accounts = new ArrayList<>();
        for (ClientConnection cc : connections) {
            accounts.add(cc.getAccount());
        }
        accounts.sort(null);
        for (String account : accounts) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(account);
        }
        GlobalServer.LOGGER.info(sb.toString());
    }
}
