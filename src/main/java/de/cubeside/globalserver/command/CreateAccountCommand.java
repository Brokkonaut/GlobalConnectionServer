package de.cubeside.globalserver.command;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;
import java.security.SecureRandom;

public class CreateAccountCommand extends ServerCommand {
    public CreateAccountCommand() {
        super("createaccount");
    }

    @Override
    public void execute(GlobalServer server, String args) {
        if (args.length() == 0 || args.indexOf(' ') > 0) {
            GlobalServer.LOGGER.info("/createaccount <name>");
            return;
        }
        String accountName = args.toLowerCase().trim();
        if (server.getAccount(accountName) != null) {
            GlobalServer.LOGGER.info("Account " + accountName + " already exists!");
            return;
        }
        String password = createRandomPassword(32);
        server.addAccount(accountName, password);
        GlobalServer.LOGGER.info("Account " + accountName + " created with password: " + password);
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
