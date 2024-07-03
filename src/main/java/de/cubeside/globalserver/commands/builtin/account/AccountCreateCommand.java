package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.security.SecureRandom;

public class AccountCreateCommand extends SubCommand {
    private GlobalServer server;

    public AccountCreateCommand(GlobalServer server) {
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
        if (server.getAccount(accountName) != null) {
            GlobalServer.LOGGER.info("Account " + accountName + " already exists!");
            return true;
        }
        String password = createRandomPassword(32);
        server.addAccount(accountName, password);
        GlobalServer.LOGGER.info("Account " + accountName + " created with password: " + password);
        return true;
    }

    public static String createRandomPassword(int length) {
        SecureRandom random = new SecureRandom();
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-+";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
