package de.cubeside.globalserver.commands.builtin.permissions;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;

public class PermissionGroupDeleteCommand extends AbstractPermissionCommandWithGroup {
    @Override
    protected boolean onCommandWithGroup(GlobalServer server, ServerCommand command, String commandString, String groupName, ArgsParser args) {
        if (args.remaining() != 0) {
            return false;
        }
        if (server.getGlobalPermissions().removeGroup(groupName)) {
            GlobalServer.LOGGER.info("Remove permission group " + groupName);
        }
        return true;
    }
}
