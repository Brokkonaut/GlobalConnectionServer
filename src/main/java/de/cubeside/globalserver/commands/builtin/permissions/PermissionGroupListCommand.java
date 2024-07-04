package de.cubeside.globalserver.commands.builtin.permissions;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;

public class PermissionGroupListCommand extends SubCommand {
    @Override
    public boolean onCommand(GlobalServer server, ServerCommand command, String commandString, ArgsParser args) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> permissionGroups = new ArrayList<>(server.getGlobalPermissions().getAllGroups());
        permissionGroups.sort((a1, a2) -> a1.compareTo(a2));
        sb.append("Permission Groups (").append(permissionGroups.size()).append("): ");
        boolean first = true;
        for (String cc : permissionGroups) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(cc);
        }
        GlobalServer.LOGGER.info(sb.toString());
        return true;
    }
}
