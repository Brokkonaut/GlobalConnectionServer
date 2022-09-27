package de.cubeside.globalserver.plugin;

public class PluginDescriptionException extends PluginLoadException {
    private static final long serialVersionUID = -7511939979685183931L;

    public PluginDescriptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginDescriptionException(String message) {
        super(message);
    }
}
