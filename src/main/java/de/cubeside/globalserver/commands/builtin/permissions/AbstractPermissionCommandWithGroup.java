package de.cubeside.globalserver.commands.builtin.permissions;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractPermissionCommandWithGroup extends SubCommand {
    @Override
    public String getUsage() {
        return "<group>";
    }

    @Override
    public boolean onCommand(GlobalServer server, ServerCommand command, String commandString, ArgsParser args) {
        if (args.remaining() < 1) {
            return false;
        }
        String groupName = args.getNext().toLowerCase().trim();
        if (!server.getGlobalPermissions().hasGroup(groupName)) {
            GlobalServer.LOGGER.info("Permission group " + groupName + " does not exist!");
            return true;
        }
        return onCommandWithGroup(server, command, commandString, groupName, args);
    }

    protected abstract boolean onCommandWithGroup(GlobalServer server, ServerCommand command, String commandString, String groupName, ArgsParser args);

    @Override
    public Collection<String> onTabComplete(GlobalServer server, ServerCommand command, ArgsParser args) {
        if (args.remaining() == 1) {
            ArrayList<String> result = new ArrayList<>();
            for (String e : server.getGlobalPermissions().getAllGroups()) {
                result.add(e);
            }
            return result;
        } else if (args.remaining() > 1) {
            String groupName = args.getNext().toLowerCase().trim();
            if (server.getGlobalPermissions().hasGroup(groupName)) {
                return onTabCompleteWithGroup(server, command, groupName, args);
            }
        }
        return List.of();
    }

    protected Collection<String> onTabCompleteWithGroup(GlobalServer server, ServerCommand command, String groupName, ArgsParser args) {
        return List.of();
    }
}
