package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ArgsParser;
import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class AccountSetRestrictedCommand extends ServerCommand {
    public AccountSetRestrictedCommand() {
        super("accountsetrestricted");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        if (args.remaining() != 2) {
            GlobalServer.LOGGER.info("/accountsetrestricted <name> <true/false>");
            return;
        }
        String accountName = args.getNext().toLowerCase().trim();
        boolean restricted = args.getNext(false);
        ClientConfig account = server.getAccount(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist!");
            return;
        }
        account.setRestricted(restricted);
        server.saveConfig();
        GlobalServer.LOGGER.info("Account " + accountName + " is now " + (account.isRestricted() ? "" : "un") + "restricted");
    }

    @Override
    public Collection<String> tabComplete(GlobalServer server, ArgsParser argsParser) {
        if (argsParser.remaining() == 1) {
            ArrayList<String> result = new ArrayList<>();
            for (ClientConfig e : server.getAccounts()) {
                result.add(e.getLogin());
            }
            return result;
        } else if (argsParser.remaining() == 2) {
            return Arrays.asList("true", "false");
        }
        return null;
    }
}
