package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.CommandRouterCommand;

public class AccountCommand extends CommandRouterCommand {
    public AccountCommand(GlobalServer server) {
        super("account");
        addCommandMapping(new AccountListCommand(server), "list");
        addCommandMapping(new AccountInfoCommand(server), "info");
        addCommandMapping(new AccountCreateCommand(server), "create");
        addCommandMapping(new AccountSetPasswordCommand(server), "setpassword");
        addCommandMapping(new AccountSetRestrictedCommand(server), "setrestriced");
        addCommandMapping(new AccountAddAllowedChannelCommand(server), "addallowedchannel");
        addCommandMapping(new AccountRemoveAllowedChannelCommand(server), "removeallowedchannel");
    }
}
