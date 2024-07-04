package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import java.util.Collection;

public class AccountAddGroupCommand extends SubCommand {
    @Override
    public String getUsage() {
        return "<account> <group>";
    }

    @Override
    public boolean onCommand(GlobalServer server, ServerCommand command, String commandString, ArgsParser args) {
        if (args.remaining() != 2) {
            return false;
        }
        String accountName = args.getNext().toLowerCase().trim();
        String group = args.getNext();
        ClientConfig account = server.getAccount(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist!");
            return true;
        }
        account.getGroups().add(group);
        server.saveConfig();
        GlobalServer.LOGGER.info("Account " + accountName + " has now group " + group + ".");
        server.reloadGroupsForAccount(accountName);
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
        }
        return null;
    }
}
