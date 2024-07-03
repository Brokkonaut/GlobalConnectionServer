package de.cubeside.globalserver.commands;

import de.cubeside.globalserver.ServerCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.cubesideutils.commands.PermissionRequirer;
import java.util.Collection;
import java.util.Collections;

public abstract class SubCommand implements PermissionRequirer {
    public abstract boolean onCommand(ServerCommand command, String commandString, ArgsParser args);

    public Collection<String> onTabComplete(ServerCommand command, ArgsParser args) {
        return Collections.emptyList();
    }

    public String getUsage() {
        return "";
    }
}