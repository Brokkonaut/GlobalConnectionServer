package de.cubeside.globalserver.commands.builtin.permissions;

import de.cubeside.globalserver.commands.CommandRouterCommand;

public class PermissionGroupCommand extends CommandRouterCommand {
    public PermissionGroupCommand() {
        super("permissiongroup");
        addCommandMapping(new PermissionGroupReloadCommand(), "reload");
        addCommandMapping(new PermissionGroupListCommand(), "list");
        addCommandMapping(new PermissionGroupInfoCommand(), "info");
        addCommandMapping(new PermissionGroupCreateCommand(), "create");
        addCommandMapping(new PermissionGroupDeleteCommand(), "delete");
        addCommandMapping(new PermissionGroupSetPriorityCommand(), "setpriority");
        addCommandMapping(new PermissionGroupAddPermissionCommand(), "addpermission");
        addCommandMapping(new PermissionGroupRemovePermissionCommand(), "removepermission");
    }
}
