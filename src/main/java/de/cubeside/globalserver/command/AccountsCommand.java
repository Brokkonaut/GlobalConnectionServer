package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ArgsParser;
import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;
import java.util.ArrayList;

public class AccountsCommand extends ServerCommand {
    public AccountsCommand() {
        super("accounts");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
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
    }
}
