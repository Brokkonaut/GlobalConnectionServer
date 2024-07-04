package de.cubeside.globalserver.commands.builtin.account;

import de.cubeside.globalserver.commands.CommandRouterCommand;

public class AccountCommand extends CommandRouterCommand {
    public AccountCommand() {
        super("account");
        addCommandMapping(new AccountListCommand(), "list");
        addCommandMapping(new AccountInfoCommand(), "info");
        addCommandMapping(new AccountCreateCommand(), "create");
        addCommandMapping(new AccountDeleteCommand(), "delete");
        addCommandMapping(new AccountSetPasswordCommand(), "setpassword");
        addCommandMapping(new AccountSetRestrictedCommand(), "setrestriced");
        addCommandMapping(new AccountAddAllowedChannelCommand(), "addallowedchannel");
        addCommandMapping(new AccountRemoveAllowedChannelCommand(), "removeallowedchannel");
        addCommandMapping(new AccountAddGroupCommand(), "addgroup");
        addCommandMapping(new AccountRemoveGroupCommand(), "removegroup");
        addCommandMapping(new AccountTestPermissionCommand(), "testpermission");
    }
}
