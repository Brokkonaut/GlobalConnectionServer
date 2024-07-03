package de.cubeside.globalserver.commands;

import de.cubeside.globalserver.GlobalServer;
import de.iani.cubesideutils.Pair;
import de.iani.cubesideutils.StringUtilCore;
import de.iani.cubesideutils.commands.AbstractCommandRouter;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

public class CommandRouterCommand extends AbstractCommandRouter<SubCommand, Void> implements ServerCommand {

    public static final String UNKNOWN_COMMAND_MESSAGE = "Unknown command. Type \"/help\" for help.";

    private final String command;

    public CommandRouterCommand(String command) {
        super(true);
        this.command = command;
    }

    @Override
    public String getCommand() {
        return command;
    }

    public SubCommand getSubCommand(String path) {
        String[] args = path.split(" ");
        Pair<CommandMap, Integer> commandMapAndArg = matchCommandMap(null, args);
        CommandMap currentMap = commandMapAndArg.first;
        int nr = commandMapAndArg.second;
        return nr == args.length ? currentMap.executor : null;
    }

    @Override
    public Collection<String> tabComplete(GlobalServer server, ArgsParser argsParser) {
        String[] args = argsParser.toArray();

        Pair<CommandMap, Integer> commandMapAndArg = matchCommandMap(null, args, 1);
        CommandMap currentMap = commandMapAndArg.first;
        int nr = commandMapAndArg.second;

        String partial = args.length > 0 ? args[args.length - 1] : "";
        Collection<String> options = null;
        List<String> optionsList = null;
        // get tabcomplete options from command
        if (currentMap.executor != null) {
            options = currentMap.executor.onTabComplete(server, this, new ArgsParser(args, nr));
        } else {
            options = Collections.emptyList();
        }
        // get tabcomplete options from subcommands
        if (nr == args.length - 1 && currentMap.subCommands != null) {
            for (Entry<String, CommandMap> e : currentMap.subCommands.entrySet()) {
                String key = e.getKey();
                if (StringUtilCore.startsWithIgnoreCase(key, partial)) {
                    CommandMap subcmd = e.getValue();
                    if (isAnySubCommandDisplayable(subcmd)) {
                        if (optionsList == null) {
                            optionsList = options == null ? new ArrayList<>() : new ArrayList<>(options);
                            options = optionsList;
                        }
                        optionsList.add(key);
                    }
                }
            }
        }
        if (options != null) {
            optionsList = StringUtilCore.copyPartialMatches(partial, options);
            Collections.sort(optionsList);
        }
        return optionsList;
    }

    @Override
    public void execute(GlobalServer server, ArgsParser argsParser) {
        String[] args = argsParser.toArray();
        Pair<CommandMap, Integer> commandMapAndArg = matchCommandMap(null, args);
        CommandMap currentMap = commandMapAndArg.first;
        int nr = commandMapAndArg.second;

        // execute this?
        SubCommand toExecute = currentMap.executor;
        if (toExecute != null) {
            if (toExecute.onCommand(server, this, getCommandString(currentMap), new ArgsParser(args, nr))) {
                return;
            } else {
                showHelp(args);
                return;
            }
        }
        // show valid cmds
        showHelp(currentMap);
    }

    private String getCommandString(CommandMap currentMap) {
        StringBuilder prefixBuilder = new StringBuilder();
        prefixBuilder.append('/').append(command).append(' ');
        ArrayList<CommandMap> hierarchy = new ArrayList<>();
        CommandMap map = currentMap;
        while (map != null) {
            hierarchy.add(map);
            map = map.parent;
        }
        for (int i = hierarchy.size() - 2; i >= 0; i--) {
            prefixBuilder.append(hierarchy.get(i).name).append(' ');
        }
        return prefixBuilder.toString();
    }

    public void showHelp(String[] args) {
        Pair<CommandMap, Integer> commandMapAndArg = matchCommandMap(null, args);
        CommandMap currentMap = commandMapAndArg.first;
        showHelp(currentMap);
    }

    private void showHelp(CommandMap currentMap) {
        if (currentMap.subCommands != null) {
            String prefix = getCommandString(currentMap);
            for (CommandMap subcmd : currentMap.subcommandsOrdered) {
                String key = subcmd.name;
                if (subcmd.executor == null) {
                    // hat weitere subcommands
                    if (isAnySubCommandDisplayable(subcmd)) {
                        GlobalServer.LOGGER.info(prefix + key + " ...");
                    }
                } else {
                    GlobalServer.LOGGER.info(prefix + key + " " + subcmd.executor.getUsage());
                }
            }
        }
        if (currentMap.executor != null) {
            SubCommand executor = currentMap.executor;
            String prefix = getCommandString(currentMap);
            GlobalServer.LOGGER.info(prefix + executor.getUsage());
        }
    }

    private boolean isAnySubCommandDisplayable(CommandMap cmd) {
        if (cmd.executor != null) {
            return true;
        }
        if (cmd.subcommandsOrdered == null) {
            return false;
        }
        return !cmd.subcommandsOrdered.isEmpty();
    }
}
