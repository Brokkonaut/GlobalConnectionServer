package de.cubeside.globalserver.plugin;

public class PluginLoadException extends Exception {
    private static final long serialVersionUID = 3150343498598575493L;

    public PluginLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginLoadException(String message) {
        super(message);
    }
}
