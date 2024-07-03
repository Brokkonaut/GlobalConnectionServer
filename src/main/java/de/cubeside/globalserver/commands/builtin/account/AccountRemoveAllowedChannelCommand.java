package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import java.util.Collection;

public class AccountRemoveAllowedChannelCommand extends SubCommand {
    private GlobalServer server;

    public AccountRemoveAllowedChannelCommand(GlobalServer server) {
        this.server = server;
    }

    @Override
    public String getUsage() {
        return "<account> <channel>";
    }

    @Override
    public boolean onCommand(ServerCommand command, String commandString, ArgsParser args) {
        if (args.remaining() != 2) {
            return false;
        }
        String accountName = args.getNext().toLowerCase().trim();
        String channel = args.getNext();
        ClientConfig account = server.getAccount(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist!");
            return true;
        }
        account.getAllowedChannels().remove(channel);
        server.saveConfig();
        GlobalServer.LOGGER.info("Channel " + channel + " is no longer allowed for account " + accountName);
        return true;
    }

    @Override
    public Collection<String> onTabComplete(ServerCommand command, ArgsParser args) {
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
