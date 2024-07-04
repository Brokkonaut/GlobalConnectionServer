package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.ClientConnection;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import java.util.Collection;

public class AccountTestPermissionCommand extends SubCommand {
    @Override
    public String getUsage() {
        return "<account> <permission>";
    }

    @Override
    public boolean onCommand(GlobalServer server, ServerCommand command, String commandString, ArgsParser args) {
        if (args.remaining() != 2) {
            return false;
        }
        String accountName = args.getNext().toLowerCase().trim();
        String permission = args.getNext();
        ClientConnection account = server.getConnection(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist or is not online!");
            return true;
        }
        boolean result = account.hasPermission(permission);
        GlobalServer.LOGGER.info("Permission check with account " + accountName + " for " + permission + " is: " + result);
        return true;
    }

    @Override
    public Collection<String> onTabComplete(GlobalServer server, ServerCommand command, ArgsParser args) {
        if (args.remaining() == 1) {
            ArrayList<String> result = new ArrayList<>();
            for (ClientConfig e : server.getAccounts()) {
                result.add(e.getLogin());
            }
            return result;
        } else if (args.remaining() == 2) {
            ClientConfig account = server.getAccount(args.getNext());
            if (account != null) {
                return account.getAllowedChannels();
            }
        }
        return null;
    }
}
