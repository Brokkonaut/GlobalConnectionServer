package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import java.util.Collection;

public class AccountSetPasswordCommand extends SubCommand {
    @Override
    public String getUsage() {
        return "<account>";
    }

    @Override
    public boolean onCommand(GlobalServer server, ServerCommand command, String commandString, ArgsParser args) {
        if (args.remaining() != 1) {
            return false;
        }
        String accountName = args.getNext().toLowerCase().trim();
        ClientConfig account = server.getAccount(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist!");
            return true;
        }
        String password = AccountCreateCommand.createRandomPassword(32);
        account.setPassword(password);
        server.saveConfig();
        GlobalServer.LOGGER.info("Account " + accountName + " now has password: " + password);
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
