package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;
import java.util.Collection;

public class AccountsCommand extends ServerCommand {
    public AccountsCommand() {
        super("accounts");
    }

    @Override
    public void execute(GlobalServer server, String args) {
        StringBuilder sb = new StringBuilder();
        Collection<ClientConfig> accounts = server.getAccounts();
        sb.append("Accounts (").append(accounts.size()).append("): ");
        boolean first = true;
        for (ClientConfig cc : accounts) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(cc.getLogin());
        }
        GlobalServer.LOGGER.info(sb.toString());
    }
}
