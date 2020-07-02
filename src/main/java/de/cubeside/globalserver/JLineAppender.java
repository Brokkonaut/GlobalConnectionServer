package de.cubeside.globalserver;

import java.io.PrintStream;
import java.io.Serializable;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "JLineAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class JLineAppender extends AbstractAppender {
    private PrintStream realSystemOut;

    protected JLineAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
        realSystemOut = System.out;
    }

    @PluginFactory
    public static JLineAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Filter") Filter filter, @PluginElement("Layout") Layout<? extends Serializable> layout) {
        return new JLineAppender(name, filter, layout);
    }

    @Override
    public void append(LogEvent event) {
        String message = getLayout().toSerializable(event).toString();
        Console console = GlobalServer.getConsole();
        if (console != null) {
            console.appendOutput(message);
        } else {
            realSystemOut.print(message);
        }
    }
}