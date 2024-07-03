package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.CommandRouterCommand;

public class AccountCommand extends CommandRouterCommand {
    public AccountCommand(GlobalServer server) {
        super("account");
        addCommandMapping(new AccountListCommand(), "list");
        addCommandMapping(new AccountInfoCommand(), "info");
        addCommandMapping(new AccountCreateCommand(), "create");
        addCommandMapping(new AccountSetPasswordCommand(), "setpassword");
        addCommandMapping(new AccountSetRestrictedCommand(), "setrestriced");
        addCommandMapping(new AccountAddAllowedChannelCommand(), "addallowedchannel");
        addCommandMapping(new AccountRemoveAllowedChannelCommand(), "removeallowedchannel");
    }
}
