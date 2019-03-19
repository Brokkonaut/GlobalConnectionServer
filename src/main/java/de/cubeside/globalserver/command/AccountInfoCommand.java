package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;

public class AccountInfoCommand extends ServerCommand {
    public AccountInfoCommand() {
        super("accountinfo");
    }

    @Override
    public void execute(GlobalServer server, String args) {
        if (args.length() == 0 || args.indexOf(' ') > 0) {
            GlobalServer.LOGGER.info("/accountinfo <name>");
            return;
        }
        String accountName = args.toLowerCase().trim();
        ClientConfig account = server.getAccount(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist!");
            return;
        }
        GlobalServer.LOGGER.info("Account " + account.getLogin() + " has password: " + account.getPassword());
    }
}
