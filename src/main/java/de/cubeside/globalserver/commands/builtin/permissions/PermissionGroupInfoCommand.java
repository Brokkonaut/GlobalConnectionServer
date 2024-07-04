package de.cubeside.globalserver.commands.builtin.permissions;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.Map;
import java.util.Map.Entry;

public class PermissionGroupInfoCommand extends AbstractPermissionCommandWithGroup {
    @Override
    protected boolean onCommandWithGroup(GlobalServer server, ServerCommand command, String commandString, String groupName, ArgsParser args) {
        if (args.remaining() != 0) {
            return false;
        }
        int prio = server.getGlobalPermissions().getGroupPriority(groupName);
        Map<String, Boolean> perms = server.getGlobalPermissions().getGroupPermissions(groupName);

        String s = "Permission Group " + groupName;
        GlobalServer.LOGGER.info(s);
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i > 0; i--) {
            sb.append("=");
        }
        GlobalServer.LOGGER.info(sb.toString());
        GlobalServer.LOGGER.info("  Priority: " + prio);
        GlobalServer.LOGGER.info("  Permissions:");
        for (Entry<String, Boolean> e : perms.entrySet()) {
            GlobalServer.LOGGER.info("    " + e.getKey() + ": " + e.getValue());
        }
        return true;
    }
}
