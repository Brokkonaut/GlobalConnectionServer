package de.cubeside.globalserver.event;

import java.util.ArrayList;
import java.util.Comparator;

public class ClassEventHandlerList<E extends Event> {
    private final Comparator<? super AbstractEventHandler<? super E>> HANDLER_COMPARATOR = (a, b) -> a.getPriority() - b.getPriority();
    private final Class<? extends Event> eventClass;
    private final ArrayList<ClassEventHandlerList<? extends E>> subclasses;
    private final ArrayList<AbstractEventHandler<? super E>> handlers;
    private volatile AbstractEventHandler<? super E>[] handlerList;

    public ClassEventHandlerList(Class<E> eventClass) {
        this.eventClass = eventClass;
        this.subclasses = new ArrayList<>();
        this.handlers = new ArrayList<>();
    }

    public void addSubclass(ClassEventHandlerList<? extends E> list) {
        subclasses.add(list);
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    public synchronized void registerHandler(AbstractEventHandler<? super E> handler) {
        handlers.add(handler);
        handlers.sort(HANDLER_COMPARATOR);
        handlerList = null;
        if (subclasses.size() > 0) {
            for (ClassEventHandlerList<? extends E> subclass : subclasses) {
                subclass.registerHandler(handler);
            }
        }
    }

    public synchronized void removeHandler(AbstractEventHandler<? super E> handler) {
        handlers.remove(handler);
        handlerList = null;
        if (subclasses.size() > 0) {
            for (ClassEventHandlerList<? extends E> subclass : subclasses) {
                subclass.removeHandler(handler);
            }
        }
    }

    public AbstractEventHandler<? super E>[] getEventHandlers() {
        AbstractEventHandler<? super E>[] list = handlerList;
        if (list != null) {
            return list;
        }
        return recreateHandlerList();
    }

    private synchronized AbstractEventHandler<? super E>[] recreateHandlerList() {
        if (handlerList != null) {
            return handlerList;
        }
        return handlerList = handlers.toArray(new AbstractEventHandler[handlers.size()]);
    }
}
