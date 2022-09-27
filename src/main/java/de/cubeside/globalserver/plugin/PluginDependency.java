package de.cubeside.globalserver.plugin;

public record PluginDependency(String plugin, String[] version, LoadOrder loadOrder, Type type) {
    public enum LoadOrder {
        BEFORE,
        AFTER,
        ANY
    }

    public enum Type {
        REQUIRED,
        OPTIONAL
    }
}
