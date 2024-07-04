package de.cubeside.globalserver.commands.builtin.permissions;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.Collection;
import java.util.List;

public class PermissionGroupRemovePermissionCommand extends AbstractPermissionCommandWithGroup {
    @Override
    public String getUsage() {
        return super.getUsage() + " <permission>";
    }

    @Override
    protected boolean onCommandWithGroup(GlobalServer server, ServerCommand command, String commandString, String groupName, ArgsParser args) {
        if (args.remaining() != 1) {
            return false;
        }
        String permission = args.getNext("");
        if (server.getGlobalPermissions().removeGroupPermission(groupName, permission)) {
            GlobalServer.LOGGER.info("Removed permission from group " + groupName + ": " + permission);
        }
        return true;
    }

    @Override
    protected Collection<String> onTabCompleteWithGroup(GlobalServer server, ServerCommand command, String groupName, ArgsParser args) {
        if (args.remaining() == 1) {
            return server.getGlobalPermissions().getGroupPermissions(groupName).keySet();
        }
        return List.of();
    }
}
