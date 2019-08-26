package de.cubeside.globalserver.command;

import de.cubeside.globalserver.ArgsParser;
import de.cubeside.globalserver.ClientConfig;
import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.ServerCommand;

public class AccountAddAllowedChannelCommand extends ServerCommand {
    public AccountAddAllowedChannelCommand() {
        super("accountaddallowedchannel");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        if (args.remaining() != 2) {
            GlobalServer.LOGGER.info("/accountaddallowedchannel <name> <channel>");
            return;
        }
        String accountName = args.getNext().toLowerCase().trim();
        String channel = args.getNext();
        ClientConfig account = server.getAccount(accountName);
        if (account == null) {
            GlobalServer.LOGGER.info("Account " + accountName + " does not exist!");
            return;
        }
        account.getAllowedChannels().add(channel);
        server.saveConfig();
        GlobalServer.LOGGER.info("Channel " + channel + " is now allowed for account " + accountName);
    }
}
