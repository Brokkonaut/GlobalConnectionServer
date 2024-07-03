package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import java.util.Collection;

public class AccountInfoCommand extends SubCommand {
    private GlobalServer server;

    public AccountInfoCommand(GlobalServer server) {
        this.server = server;
    }

    @Override
    public String getUsage() {
        return "<account>";
    }

    @Override
    public boolean onCommand(ServerCommand command, String commandString, ArgsParser args) {
        if (args.remaining() != 1) {
            return false;
        }
        String accountName = args.getNext().toLowerCase().trim();
        ClientConfig account = server.getAccount(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist!");
            return true;
        }
        String s = "Account " + account.getLogin();
        GlobalServer.LOGGER.info(s);
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i > 0; i--) {
            sb.append("=");
        }
        GlobalServer.LOGGER.info(sb.toString());
        GlobalServer.LOGGER.info("  Password: " + account.getPassword());
        GlobalServer.LOGGER.info("  Restricted: " + account.isRestricted());
        if (account.isRestricted()) {
            GlobalServer.LOGGER.info("  Allowed Channels:");
            for (String s2 : account.getAllowedChannels()) {
                GlobalServer.LOGGER.info("    " + s2);
            }
            if (account.getAllowedChannels().isEmpty()) {
                GlobalServer.LOGGER.info("    " + "(none)");
            }
        }
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
        }
        return null;
    }
}
