package de.cubeside.globalserver.commands.builtin.permissions;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;

public class PermissionGroupSetPriorityCommand extends AbstractPermissionCommandWithGroup {
    @Override
    public String getUsage() {
        return super.getUsage() + " <priority>";
    }

    @Override
    protected boolean onCommandWithGroup(GlobalServer server, ServerCommand command, String commandString, String groupName, ArgsParser args) {
        if (args.remaining() != 1) {
            return false;
        }
        int prio = args.getNext(Integer.MIN_VALUE);
        if (prio == Integer.MIN_VALUE) {
            GlobalServer.LOGGER.info("Invalid priority, must be an int");
            return true;
        }
        if (server.getGlobalPermissions().setGroupPriority(groupName, prio)) {
            GlobalServer.LOGGER.info("Updated priority of group " + groupName + " to " + prio);
        }
        return true;
    }
}
