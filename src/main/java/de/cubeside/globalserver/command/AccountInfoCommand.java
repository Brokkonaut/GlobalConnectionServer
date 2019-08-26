package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ArgsParser;
import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;

public class AccountInfoCommand extends ServerCommand {
    public AccountInfoCommand() {
        super("accountinfo");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        if (args.remaining() != 1) {
            GlobalServer.LOGGER.info("/accountinfo <name>");
            return;
        }
        String accountName = args.getNext().toLowerCase().trim();
        ClientConfig account = server.getAccount(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist!");
            return;
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
    }
}
