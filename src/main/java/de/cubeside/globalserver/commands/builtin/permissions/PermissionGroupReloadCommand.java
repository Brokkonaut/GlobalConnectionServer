package de.cubeside.globalserver.commands.builtin.permissions;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.commands.ServerCommand;
import de.cubeside.globalserver.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;

public class PermissionGroupReloadCommand extends SubCommand {
    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean onCommand(GlobalServer server, ServerCommand command, String commandString, ArgsParser args) {
        if (args.remaining() != 0) {
            return false;
        }
        server.getGlobalPermissions().reload();
        return true;
    }
}
