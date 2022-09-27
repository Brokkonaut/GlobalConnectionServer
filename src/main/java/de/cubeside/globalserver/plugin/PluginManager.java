package de.cubeside.globalserver.plugin;

import de.cubeside.globalserver.plugin.PluginDependency.LoadOrder;
import de.cubeside.globalserver.plugin.PluginDependency.Type;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PluginManager {
    public final static Logger LOGGER = LogManager.getLogger("PluginManager");

    private static final Function<PluginContext, HashSet<PluginContext>> HASH_SET_CREATOR = (c) -> new HashSet<>();
    private File pluginFolder;

    public PluginManager(File pluginFolder) {
        this.pluginFolder = pluginFolder;
    }

    public void loadPlugins() throws PluginLoadException {
        LinkedHashMap<String, PluginContext> pluginContexts = new LinkedHashMap<>();
        File[] pluginFiles = pluginFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        Arrays.sort(pluginFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));// guarantee a consistent load order
        for (File jarFile : pluginFiles) {
            PluginDescription description = new PluginDescription(jarFile);
            if (pluginContexts.put(description.getName(), new PluginContext(description)) != null) {
                throw new PluginLoadException("Duplicate plugin " + description.getName());
            }
        }

        // check for missing dependencies
        HashMap<PluginContext, HashSet<PluginContext>> allLoadBefore = new HashMap<>();
        for (PluginContext context : pluginContexts.values()) {
            HashSet<PluginContext> dependencies = new HashSet<>();
            for (PluginDependency dependency : context.getDescription().getDependencies()) {
                PluginContext resolvedDependency = pluginContexts.get(dependency.plugin());
                if (resolvedDependency != null) {
                    addDependenciesRecursive(pluginContexts, dependencies, resolvedDependency, dependency.loadOrder(), context, allLoadBefore);
                } else if (dependency.type() == Type.REQUIRED) {
                    throw new PluginLoadException("Plugin " + context.getDescription().getName() + " is missing the required dependency " + dependency.plugin());
                }
            }

            // set classloader alternatives
            ArrayList<PluginClassLoader> dependencyLoaders = new ArrayList<>();
            for (PluginContext otherContext : dependencies) {
                if (otherContext != context) {
                    dependencyLoaders.add(otherContext.getClassLoader());
                }
            }
            // System.out.println("Dependencies for " + context.getDescription().getName() + " " + context.getDescription().getVersion() + ": " + dependencyLoaders.stream().map(d -> d.getPlugin().getName()).toList());
            context.getClassLoader().setDependencyClassLoades(dependencyLoaders);
        }

        // check for circular dependencies
        ArrayList<PluginContext> notOrderedIn = new ArrayList<>();
        for (PluginContext context : pluginContexts.values()) {
            HashSet<PluginContext> deps = allLoadBefore.get(context);
            if (deps != null && deps.contains(context)) {
                throw new PluginLoadException("Circular dependencies for plugin " + context.getDescription().getName());
            }
            notOrderedIn.add(context);
        }

        // reorder plugins
        HashSet<PluginContext> orderedIn = new HashSet<>();
        LinkedHashMap<String, PluginContext> newPluginContexts = new LinkedHashMap<>();
        while (!notOrderedIn.isEmpty()) {
            Iterator<PluginContext> it = notOrderedIn.iterator();
            nextPlugin: while (it.hasNext()) {
                PluginContext plugin = it.next();
                HashSet<PluginContext> loadBeforeThis = allLoadBefore.get(plugin);
                if (loadBeforeThis != null) {
                    for (PluginContext e : loadBeforeThis) {
                        if (!orderedIn.contains(e)) {
                            continue nextPlugin;
                        }
                    }
                }
                orderedIn.add(plugin);
                newPluginContexts.put(plugin.getDescription().getName(), plugin);
                it.remove();
                // System.out.println(plugin.getDescription().getName());
            }
        }
        pluginContexts = newPluginContexts;

        // start loading the plugins
        for (PluginContext context : pluginContexts.values()) {
            try {
                LOGGER.info("Loading plugin " + context.getDescription().getName());
                context.createMainClassInstance();
            } catch (Exception e) {
                throw new PluginLoadException("Could not load the plugin " + context.getDescription().getName(), e);
            }
        }

    }

    private void addDependenciesRecursive(HashMap<String, PluginContext> pluginContexts, HashSet<PluginContext> dependencies, PluginContext dependency, LoadOrder loadOrder, PluginContext initialPlugin, HashMap<PluginContext, HashSet<PluginContext>> allLoadBefore) {
        if (loadOrder == LoadOrder.BEFORE) {
            HashSet<PluginContext> set = allLoadBefore.computeIfAbsent(initialPlugin, HASH_SET_CREATOR);
            set.add(dependency);
        } else if (loadOrder == LoadOrder.AFTER) {
            HashSet<PluginContext> set = allLoadBefore.computeIfAbsent(dependency, HASH_SET_CREATOR);
            set.add(initialPlugin);
        }
        if (dependencies.add(dependency)) {
            for (PluginDependency nextDependency : dependency.getDescription().getDependencies()) {
                PluginContext resolvedDependency = pluginContexts.get(nextDependency.plugin());
                if (resolvedDependency != null) {
                    addDependenciesRecursive(pluginContexts, dependencies, resolvedDependency, nextDependency.loadOrder() == loadOrder ? loadOrder : LoadOrder.ANY, initialPlugin, allLoadBefore);
                }
            }
        }
    }
}
