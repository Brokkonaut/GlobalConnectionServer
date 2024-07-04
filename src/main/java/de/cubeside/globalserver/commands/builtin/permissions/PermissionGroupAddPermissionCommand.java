package de.cubeside.globalserver.commands.builtin.permissions;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.Collection;
import java.util.List;

public class PermissionGroupAddPermissionCommand extends AbstractPermissionCommandWithGroup {
    @Override
    public String getUsage() {
        return super.getUsage() + " <permission> [true/false]";
    }

    @Override
    protected boolean onCommandWithGroup(GlobalServer server, ServerCommand command, String commandString, String groupName, ArgsParser args) {
        if (args.remaining() != 1 && args.remaining() != 2) {
            return false;
        }
        String permission = args.getNext("");
        boolean v = true;
        if (args.remaining() > 0) {
            String value = args.getNext("").toLowerCase();
            if (value.equals("true")) {
                v = true;
            } else if (value.equals("false")) {
                v = false;
            } else {
                GlobalServer.LOGGER.info("Invalid value must be true/false");
                return true;
            }
        }
        if (server.getGlobalPermissions().addGroupPermission(groupName, permission, v)) {
            GlobalServer.LOGGER.info("Add permission to group " + groupName + ": " + permission + "=" + v);
        }
        return true;
    }

    @Override
    protected Collection<String> onTabCompleteWithGroup(GlobalServer server, ServerCommand command, String groupName, ArgsParser args) {
        if (args.remaining() == 2) {
            return List.of("true", "false");
        }
        return List.of();
    }
}
