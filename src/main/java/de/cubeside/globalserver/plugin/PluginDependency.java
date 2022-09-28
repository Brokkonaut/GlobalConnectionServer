package de.cubeside.globalserver.plugin;

import com.vdurmont.semver4j.Requirement;

public record PluginDependency(String plugin, Requirement[] version, LoadOrder loadOrder, Type type) {
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
