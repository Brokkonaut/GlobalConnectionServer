package de.cubeside.globalserver.command;

import de.cubeside.globalserver.GlobalServer;
import de.cubeside.globalserver.AbstractServerCommand;
import de.cubeside.globalserver.plugin.Plugin;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;

public class PluginsCommand extends AbstractServerCommand {
    public PluginsCommand() {
        super("plugins");
    }

    @Override
    public void execute(GlobalServer server, ArgsParser args) {
        StringBuilder sb = new StringBuilder();
        ArrayList<Plugin> plugins = new ArrayList<>(server.getPluginManager().getPlugins());
        plugins.sort((a1, a2) -> a1.getDescription().getName().compareTo(a2.getDescription().getName()));
        sb.append("Plugins (").append(plugins.size()).append("): ");
        boolean first = true;
        for (Plugin plugin : plugins) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(plugin.getDescription().getName());
        }
        GlobalServer.LOGGER.info(sb.toString());
    }
}
