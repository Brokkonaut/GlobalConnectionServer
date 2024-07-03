package de.cubeside.globalserver;

import de.iani.cubesideutils.commands.ArgsParser;
import java.util.Collection;

public interface ServerCommand {
    String getCommand();

    void execute(GlobalServer server, ArgsParser args);

    Collection<String> tabComplete(GlobalServer server, ArgsParser argsParser);
}