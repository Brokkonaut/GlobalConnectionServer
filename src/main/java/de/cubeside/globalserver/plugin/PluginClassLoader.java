package de.cubeside.globalserver.plugin;

import de.cubeside.globalserver.GlobalServer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

public class PluginClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final PluginDescription plugin;
    private final GlobalServer server;
    private PluginClassLoader[] dependencyPluginClassLoaders;

    public PluginClassLoader(GlobalServer server, PluginDescription plugin, ClassLoader parent) throws MalformedURLException {
        super(new URL[] { plugin.getJarFile().toURI().toURL() }, parent);
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // PluginManager.LOGGER.info("Loading class " + name + " for " + plugin.getName());
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException ignored) {
        }

        for (PluginClassLoader loader : dependencyPluginClassLoaders) {
            try {
                return loader.superLoadClass(name, resolve);
            } catch (ClassNotFoundException ignored) {
            }
        }

        throw new ClassNotFoundException(name);
    }

    private Class<?> superLoadClass(String name, boolean resolve) throws ClassNotFoundException {
        System.out.println("Looking in plugin " + plugin.getName() + " for " + name);
        return super.loadClass(name, resolve);
    }

    void setDependencyClassLoades(Collection<PluginClassLoader> loaders) {
        this.dependencyPluginClassLoaders = loaders.toArray(new PluginClassLoader[loaders.size()]);
    }

    public PluginDescription getPlugin() {
        return plugin;
    }

    public GlobalServer getServer() {
        return server;
    }
}
