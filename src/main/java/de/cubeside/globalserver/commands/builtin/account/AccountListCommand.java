package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;

public class AccountListCommand extends SubCommand {
    private GlobalServer server;

    public AccountListCommand(GlobalServer server) {
        this.server = server;
    }

    @Override
    public boolean onCommand(ServerCommand command, String commandString, ArgsParser args) {
        StringBuilder sb = new StringBuilder();
        ArrayList<ClientConfig> accounts = new ArrayList<>(server.getAccounts());
        accounts.sort((a1, a2) -> a1.getLogin().compareTo(a2.getLogin()));
        sb.append("Accounts (").append(accounts.size()).append("): ");
        boolean first = true;
        for (ClientConfig cc : accounts) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(cc.getLogin());
            if (cc.isRestricted()) {
                sb.append(" (restricted)");
            }
        }
        GlobalServer.LOGGER.info(sb.toString());
        return true;
    }
}
