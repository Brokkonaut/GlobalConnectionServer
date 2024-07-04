package de.cubeside.globalserver.commands.builtin.permissions;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;

public class PermissionGroupCreateCommand extends SubCommand {
    @Override
    public String getUsage() {
        return "<group>";
    }

    @Override
    public boolean onCommand(GlobalServer server, ServerCommand command, String commandString, ArgsParser args) {
        if (args.remaining() != 1) {
            return false;
        }
        String groupName = args.getNext().toLowerCase().trim();
        if (server.getGlobalPermissions().hasGroup(groupName)) {
            GlobalServer.LOGGER.info("Permission group " + groupName + " already exists!");
            return true;
        }
        if (server.getGlobalPermissions().addGroup(groupName)) {
            GlobalServer.LOGGER.info("Permission group " + groupName + " created.");
        }
        return true;
    }
}
