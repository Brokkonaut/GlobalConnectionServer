package de.cubeside.globalserver.plugin;

public class PluginContextException extends PluginLoadException {
    private static final long serialVersionUID = 8976443116706583966L;

    public PluginContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginContextException(String message) {
        super(message);
    }
}
