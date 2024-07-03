package de.cubeside.globalserver;

import de.iani.cubesideutils.commands.ArgsParser;
import java.util.Collection;

public abstract class AbstractServerCommand implements ServerCommand {
    private final String cmd;

    public AbstractServerCommand(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public final String getCommand() {
        return cmd;
    }

    @Override
    public abstract void execute(GlobalServer server, ArgsParser args);

    @Override
    public Collection<String> tabComplete(GlobalServer server, ArgsParser argsParser) {
        return null;
    }
}
