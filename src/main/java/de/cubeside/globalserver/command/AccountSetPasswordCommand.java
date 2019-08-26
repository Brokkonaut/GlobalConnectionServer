package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ArgsParser;
import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;

public class AccountSetPasswordCommand extends ServerCommand {
    public AccountSetPasswordCommand() {
        super("accountsetpassword");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        if (args.remaining() != 1) {
            GlobalServer.LOGGER.info("/accountsetpassword <name>");
            return;
        }
        String accountName = args.getNext().toLowerCase().trim();
        ClientConfig account = server.getAccount(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist!");
            return;
        }
        String password = CreateAccountCommand.createRandomPassword(32);
        account.setPassword(password);
        server.saveConfig();
        GlobalServer.LOGGER.info("Account " + accountName + " now has password: " + password);
    }
}
