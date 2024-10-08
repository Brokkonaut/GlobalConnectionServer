package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class AccountSetRestrictedCommand extends SubCommand {
    @Override
    public String getUsage() {
        return "<account> <true/false>";
    }

    @Override
    public boolean onCommand(GlobalServer server, ServerCommand command, String commandString, ArgsParser args) {
        if (args.remaining() != 2) {
            return false;
        }
        String accountName = args.getNext().toLowerCase().trim();
        Boolean restricted = args.getNext(false);
        if (restricted == null) {
            return false;
        }
        ClientConfig account = server.getAccount(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist!");
            return true;
        }
        account.setRestricted(restricted);
        server.saveConfig();
        GlobalServer.LOGGER.info("Account " + accountName + " is now " + (account.isRestricted() ? "" : "un") + "restricted");
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
            return Arrays.asList("true", "false");
        }
        return null;
    }
}
